package com.baker.integration.asana.service;

import com.baker.integration.asana.config.KeycloakProperties;
import com.baker.integration.asana.exception.DamAuthenticationRequiredException;
import com.baker.integration.asana.model.auth.DamTokenInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
public class DamAuthService {

    private static final Logger log = LoggerFactory.getLogger(DamAuthService.class);

    private final WebClient keycloakWebClient;
    private final KeycloakProperties keycloakProperties;
    private final DamTokenStore tokenStore;

    public DamAuthService(@Qualifier("keycloakWebClient") WebClient keycloakWebClient,
                          KeycloakProperties keycloakProperties,
                          DamTokenStore tokenStore) {
        this.keycloakWebClient = keycloakWebClient;
        this.keycloakProperties = keycloakProperties;
        this.tokenStore = tokenStore;
    }

    public String buildAuthorizationUrl(String stateToken, String realm) {
        return UriComponentsBuilder
                .fromHttpUrl(keycloakProperties.getBaseUrl())
                .pathSegment("realms", realm, "protocol", "openid-connect", "auth")
                .queryParam("client_id", keycloakProperties.getClientId())
                .queryParam("redirect_uri", keycloakProperties.getRedirectUri())
                .queryParam("response_mode", "query")
                .queryParam("response_type", "code")
                .queryParam("scope", "openid")
                .queryParam("state", stateToken)
                .build()
                .toUriString();
    }

    @SuppressWarnings("unchecked")
    public DamTokenInfo exchangeCodeForTokens(String code, String realm) {
        String tokenUri = "/realms/" + realm + "/protocol/openid-connect/token";

        Map<String, Object> response = keycloakWebClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters
                        .fromFormData("grant_type", "authorization_code")
                        .with("code", code)
                        .with("redirect_uri", keycloakProperties.getRedirectUri())
                        .with("client_id", keycloakProperties.getClientId())
                        .with("client_secret", keycloakProperties.getClientSecret()))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("access_token")) {
            throw new RuntimeException("Failed to exchange authorization code for tokens");
        }

        String accessToken = (String) response.get("access_token");
        String refreshToken = (String) response.get("refresh_token");
        int expiresIn = (int) response.get("expires_in");

        log.info("Successfully exchanged authorization code for DAM tokens (realm: {})", realm);
        return new DamTokenInfo(accessToken, refreshToken, expiresIn, realm);
    }

    @SuppressWarnings("unchecked")
    public DamTokenInfo refreshToken(DamTokenInfo existing) {
        String tokenUri = "/realms/" + existing.getRealm() + "/protocol/openid-connect/token";

        Map<String, Object> response = keycloakWebClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters
                        .fromFormData("grant_type", "refresh_token")
                        .with("refresh_token", existing.getRefreshToken())
                        .with("client_id", keycloakProperties.getClientId())
                        .with("client_secret", keycloakProperties.getClientSecret()))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("access_token")) {
            throw new RuntimeException("Failed to refresh DAM token");
        }

        String accessToken = (String) response.get("access_token");
        String refreshToken = (String) response.get("refresh_token");
        int expiresIn = (int) response.get("expires_in");

        log.debug("Successfully refreshed DAM token (realm: {})", existing.getRealm());
        return new DamTokenInfo(accessToken, refreshToken, expiresIn, existing.getRealm());
    }

    public String getValidAccessToken(String asanaUserGid) {
        DamTokenInfo info = tokenStore.getToken(asanaUserGid).orElse(null);

        if (info == null) {
            throw new DamAuthenticationRequiredException("User not authenticated with Lytho DAM");
        }

        if (info.isExpired()) {
            try {
                DamTokenInfo refreshed = refreshToken(info);
                tokenStore.storeToken(asanaUserGid, refreshed);
                return refreshed.getAccessToken();
            } catch (Exception e) {
                log.warn("Failed to refresh DAM token for user {}, removing stale token", asanaUserGid, e);
                tokenStore.removeToken(asanaUserGid);
                throw new DamAuthenticationRequiredException("DAM session expired. Please re-authenticate.");
            }
        }

        return info.getAccessToken();
    }
}
