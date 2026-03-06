package com.baker.integration.asana.service;

import com.baker.integration.asana.config.KeycloakProperties;
import com.baker.integration.asana.model.keycloak.KeycloakTokenResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Service
public class KeycloakOidcService {

    private final KeycloakProperties keycloakProperties;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public KeycloakOidcService(KeycloakProperties keycloakProperties, ObjectMapper objectMapper) {
        this.keycloakProperties = keycloakProperties;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder().build();
    }

    public String buildAuthorizationUrl(String realm, String state) {
        validateConfig();
        return UriComponentsBuilder
                .fromHttpUrl(joinBaseAndPath("/realms/" + realm + "/protocol/openid-connect/auth"))
                .queryParam("client_id", keycloakProperties.getClientId())
                .queryParam("response_type", "code")
                .queryParam("scope", keycloakProperties.getScope())
                .queryParam("redirect_uri", keycloakProperties.getRedirectUri())
                .queryParam("state", state)
                .build(true)
                .toUriString();
    }

    public KeycloakTokenResponse exchangeCodeForTokens(String realm, String code) {
        validateConfig();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", keycloakProperties.getClientId());
        form.add("client_secret", keycloakProperties.getClientSecret());
        form.add("code", code);
        form.add("redirect_uri", keycloakProperties.getRedirectUri());
        return tokenRequest(realm, form);
    }

    public KeycloakTokenResponse refreshAccessToken(String realm, String refreshOrOfflineToken) {
        validateConfig();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", keycloakProperties.getClientId());
        form.add("client_secret", keycloakProperties.getClientSecret());
        form.add("refresh_token", refreshOrOfflineToken);
        return tokenRequest(realm, form);
    }

    public String extractUserIdFromIdToken(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new IllegalArgumentException("id_token is empty");
        }
        String[] parts = idToken.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("id_token format is invalid");
        }

        try {
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode node = objectMapper.readTree(new String(payload, StandardCharsets.UTF_8));
            JsonNode subNode = node.get("sub");
            if (subNode == null || subNode.asText().isBlank()) {
                throw new IllegalArgumentException("id_token does not include sub");
            }
            return subNode.asText();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse id_token", e);
        }
    }

    private KeycloakTokenResponse tokenRequest(String realm, MultiValueMap<String, String> form) {
        try {
            KeycloakTokenResponse response = webClient.post()
                    .uri(joinBaseAndPath("/realms/" + realm + "/protocol/openid-connect/token"))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(form))
                    .retrieve()
                    .bodyToMono(KeycloakTokenResponse.class)
                    .block(Duration.ofSeconds(keycloakProperties.getTimeoutSeconds()));

            if (response == null || response.getAccessToken() == null || response.getRefreshToken() == null) {
                throw new IllegalArgumentException("Keycloak token response was incomplete");
            }

            return response;
        } catch (Exception e) {
            throw new IllegalArgumentException("Keycloak token request failed", e);
        }
    }

    private String joinBaseAndPath(String path) {
        String base = keycloakProperties.getBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + path;
    }

    private void validateConfig() {
        if (isBlank(keycloakProperties.getBaseUrl())
                || isBlank(keycloakProperties.getClientId())
                || isBlank(keycloakProperties.getClientSecret())
                || isBlank(keycloakProperties.getRedirectUri())) {
            throw new IllegalStateException("Keycloak configuration is incomplete");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
