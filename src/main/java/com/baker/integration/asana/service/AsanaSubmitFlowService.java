package com.baker.integration.asana.service;

import com.baker.integration.asana.config.AsanaAppProperties;
import com.baker.integration.asana.model.asana.AsanaSubmitRequest;
import com.baker.integration.asana.model.connection.AsanaConnection;
import com.baker.integration.asana.model.connection.TenantContext;
import com.baker.integration.asana.model.keycloak.KeycloakTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AsanaSubmitFlowService {

    private static final Logger log = LoggerFactory.getLogger(AsanaSubmitFlowService.class);

    private final TenantResolutionService tenantResolutionService;
    private final AsanaConnectionService asanaConnectionService;
    private final AsanaLinkStateService asanaLinkStateService;
    private final KeycloakOidcService keycloakOidcService;
    private final AsanaApiService asanaApiService;
    private final AsanaAppProperties asanaAppProperties;
    private final ZapierWebhookService zapierWebhookService;

    public AsanaSubmitFlowService(TenantResolutionService tenantResolutionService,
                                  AsanaConnectionService asanaConnectionService,
                                  AsanaLinkStateService asanaLinkStateService,
                                  KeycloakOidcService keycloakOidcService,
                                  AsanaApiService asanaApiService,
                                  AsanaAppProperties asanaAppProperties,
                                  ZapierWebhookService zapierWebhookService) {
        this.tenantResolutionService = tenantResolutionService;
        this.asanaConnectionService = asanaConnectionService;
        this.asanaLinkStateService = asanaLinkStateService;
        this.keycloakOidcService = keycloakOidcService;
        this.asanaApiService = asanaApiService;
        this.asanaAppProperties = asanaAppProperties;
        this.zapierWebhookService = zapierWebhookService;
    }

    public Map<String, Object> handleSubmit(AsanaSubmitRequest submitRequest,
                                            List<String> selectedAttachments,
                                            String baseUrl) {
        Optional<AsanaConnection> linkedConnection = asanaConnectionService.findLinkedByWorkspaceAndUser(
                submitRequest.getWorkspace(), submitRequest.getUser());

        if (linkedConnection.isEmpty()) {
            TenantContext tenantContext = tenantResolutionService.resolve(submitRequest.getWorkspace());
            return buildConnectAccountResponse(submitRequest, tenantContext, baseUrl, "Connect account to continue");
        }

        AsanaConnection connection = linkedConnection.get();
        try {
            TenantContext tenantContext = tenantResolutionService.resolve(submitRequest.getWorkspace());
            if (!tenantContext.getTenantId().equals(connection.getTenantId())) {
                asanaConnectionService.markNeedsReauth(connection);
                return buildConnectAccountResponse(
                        submitRequest,
                        tenantContext,
                        baseUrl,
                        "Connection tenant mismatch. Reconnect account."
                );
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
            TenantContext tenantContext = tenantResolutionService.resolve(submitRequest.getWorkspace());
            return buildConnectAccountResponse(
                    submitRequest,
                    tenantContext,
                    baseUrl,
                    "Connection expired. Reconnect account."
            );
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

    private Map<String, Object> buildConnectAccountResponse(AsanaSubmitRequest submitRequest,
                                                            TenantContext tenantContext,
                                                            String baseUrl,
                                                            String message) {
        String signedState = asanaLinkStateService.createSignedState(
                submitRequest.getWorkspace(),
                submitRequest.getUser(),
                tenantContext.getTenantId()
        );
        String connectStartUrl = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/connect/start")
                .queryParam("state", signedState)
                .build()
                .encode()
                .toUriString();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("resource_name", message);
        response.put("resource_url", connectStartUrl);
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
