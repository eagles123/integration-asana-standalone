package com.baker.integration.asana.controller;

import com.baker.integration.asana.model.connection.LinkStatePayload;
import com.baker.integration.asana.model.connection.TenantContext;
import com.baker.integration.asana.model.keycloak.KeycloakTokenResponse;
import com.baker.integration.asana.service.AsanaConnectionService;
import com.baker.integration.asana.service.AsanaLinkStateService;
import com.baker.integration.asana.service.KeycloakOidcService;
import com.baker.integration.asana.service.TenantResolutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class AsanaConnectController {

    private static final Logger log = LoggerFactory.getLogger(AsanaConnectController.class);

    private final AsanaLinkStateService asanaLinkStateService;
    private final TenantResolutionService tenantResolutionService;
    private final KeycloakOidcService keycloakOidcService;
    private final AsanaConnectionService asanaConnectionService;

    public AsanaConnectController(AsanaLinkStateService asanaLinkStateService,
                                  TenantResolutionService tenantResolutionService,
                                  KeycloakOidcService keycloakOidcService,
                                  AsanaConnectionService asanaConnectionService) {
        this.asanaLinkStateService = asanaLinkStateService;
        this.tenantResolutionService = tenantResolutionService;
        this.keycloakOidcService = keycloakOidcService;
        this.asanaConnectionService = asanaConnectionService;
    }

    @GetMapping("/connect/start")
    public ResponseEntity<Void> startConnect(@RequestParam("state") String signedState) {
        LinkStatePayload payload = asanaLinkStateService.verifyWithoutConsume(signedState);
        TenantContext tenantContext = tenantResolutionService.resolve(payload.getAsanaWorkspaceGid());

        if (!tenantContext.getTenantId().equals(payload.getTenantId())) {
            throw new IllegalArgumentException("Tenant mismatch for state payload");
        }

        String authUrl = keycloakOidcService.buildAuthorizationUrl(tenantContext.getKeycloakRealm(), signedState);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, URI.create(authUrl).toString())
                .build();
    }

    @GetMapping(value = "/connect/callback", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> connectCallback(@RequestParam("state") String signedState,
                                                  @RequestParam("code") String code) {
        try {
            LinkStatePayload payload = asanaLinkStateService.verifyAndConsume(signedState);
            TenantContext tenantContext = tenantResolutionService.resolve(payload.getAsanaWorkspaceGid());
            if (!tenantContext.getTenantId().equals(payload.getTenantId())) {
                throw new IllegalArgumentException("Tenant mismatch for callback state");
            }

            KeycloakTokenResponse tokenResponse = keycloakOidcService.exchangeCodeForTokens(
                    tenantContext.getKeycloakRealm(), code);
            String keycloakUserId = keycloakOidcService.extractUserIdFromIdToken(tokenResponse.getIdToken());

            asanaConnectionService.upsertLinkedConnection(
                    payload.getAsanaWorkspaceGid(),
                    payload.getAsanaUserGid(),
                    payload.getTenantId(),
                    tenantContext.getKeycloakRealm(),
                    keycloakUserId,
                    tokenResponse
            );

            return ResponseEntity.ok(successHtml());
        } catch (Exception e) {
            log.warn("Connect callback failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorHtml());
        }
    }

    private String successHtml() {
        return "<html><body><h2>Connected successfully</h2>"
                + "<p>Go back to Asana and retry your action.</p></body></html>";
    }

    private String errorHtml() {
        return "<html><body><h2>Connection failed</h2>"
                + "<p>Please return to Asana and try connecting again.</p></body></html>";
    }
}
