package com.baker.integration.asana.service;

import com.baker.integration.asana.config.AsanaAppProperties;
import com.baker.integration.asana.model.asana.AsanaSubmitRequest;
import com.baker.integration.asana.model.connection.AsanaConnection;
import com.baker.integration.asana.model.connection.TenantContext;
import com.baker.integration.asana.model.keycloak.KeycloakTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AsanaSubmitFlowService {

    private static final Logger log = LoggerFactory.getLogger(AsanaSubmitFlowService.class);
    private static final String HARDCODED_CONNECT_URL = "https://login.us-1.golytho.us/auth/realms/qaorange/protocol/openid-connect/auth?client_id=baker-app&redirect_uri=https%3A%2F%2Fqaorange.us-1.golytho.us%2F&state=e5f594cf-5799-49e3-91ac-96dea73f23b7&response_mode=fragment&response_type=code&scope=openid&nonce=2cfc8d0c-2280-4890-896e-6ec389104e6c";

    private final TenantResolutionService tenantResolutionService;
    private final AsanaConnectionService asanaConnectionService;
    private final KeycloakOidcService keycloakOidcService;
    private final AsanaApiService asanaApiService;
    private final AsanaAppProperties asanaAppProperties;
    private final ZapierWebhookService zapierWebhookService;

    public AsanaSubmitFlowService(TenantResolutionService tenantResolutionService,
                                  AsanaConnectionService asanaConnectionService,
                                  KeycloakOidcService keycloakOidcService,
                                  AsanaApiService asanaApiService,
                                  AsanaAppProperties asanaAppProperties,
                                  ZapierWebhookService zapierWebhookService) {
        this.tenantResolutionService = tenantResolutionService;
        this.asanaConnectionService = asanaConnectionService;
        this.keycloakOidcService = keycloakOidcService;
        this.asanaApiService = asanaApiService;
        this.asanaAppProperties = asanaAppProperties;
        this.zapierWebhookService = zapierWebhookService;
    }

    public Map<String, Object> handleSubmit(AsanaSubmitRequest submitRequest,
                                            List<String> selectedAttachments) {
        Optional<AsanaConnection> linkedConnection = asanaConnectionService.findLinkedByWorkspaceAndUser(
                submitRequest.getWorkspace(), submitRequest.getUser());

        if (linkedConnection.isEmpty()) {
            return buildConnectAccountResponse(
                    "Connect account to continue");
        }

        AsanaConnection connection = linkedConnection.get();
        try {
            TenantContext tenantContext = tenantResolutionService.resolve(submitRequest.getWorkspace());
            if (!tenantContext.getTenantId().equals(connection.getTenantId())) {
                asanaConnectionService.markNeedsReauth(connection);
                return buildConnectAccountResponse("Connection tenant mismatch. Reconnect account.");
            }
        } catch (IllegalArgumentException e) {
            log.warn("Tenant mapping unavailable for workspace {}. Skipping tenant consistency check.",
                    submitRequest.getWorkspace());
        }

        try {
            String renewableToken = asanaConnectionService.decryptRenewableToken(connection);
            KeycloakTokenResponse refreshed = keycloakOidcService.refreshAccessToken(
                    connection.getKeycloakRealm(), renewableToken);
            asanaConnectionService.rotateRenewableToken(connection, refreshed);
        } catch (Exception e) {
            log.warn("Token refresh failed for connection {}: {}", connection.getId(), e.getMessage());
            asanaConnectionService.markNeedsReauth(connection);
            return buildConnectAccountResponse("Connection expired. Reconnect account.");
        }

        if (!zapierWebhookService.isConfigured()) {
            throw new IllegalArgumentException("Zapier catch hook URL is not configured");
        }

        String accessToken = asanaAppProperties.getPersonalAccessToken();
        String userEmail = asanaApiService.getUserEmail(submitRequest.getUser(), accessToken);

        zapierWebhookService.dispatchSelectedAttachments(
                submitRequest.getTask(),
                submitRequest.getUser(),
                submitRequest.getWorkspace(),
                userEmail,
                selectedAttachments,
                submitRequest.getValues(),
                connection.getKeycloakUserId()
        );

        asanaConnectionService.markLastUsed(connection);
        return buildSuccessResponse(selectedAttachments.size() + " attachment(s) sent to Zapier");
    }

    private Map<String, Object> buildConnectAccountResponse(String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("resource_name", message);
        response.put("resource_url", HARDCODED_CONNECT_URL);
        response.put("action", "CONNECT_ACCOUNT");
        return response;
    }

    private Map<String, Object> buildSuccessResponse(String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("resource_name", message);
        response.put("resource_url", "https://app.asana.com");
        response.put("status", "ok");
        return response;
    }
}
