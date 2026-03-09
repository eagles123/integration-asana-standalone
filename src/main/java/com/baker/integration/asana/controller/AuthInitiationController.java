package com.baker.integration.asana.controller;

import com.baker.integration.asana.config.LythoAuthProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthInitiationController {

    private static final Logger log = LoggerFactory.getLogger(AuthInitiationController.class);

    private final LythoAuthProperties lythoAuthProperties;
    private final ObjectMapper objectMapper;

    public AuthInitiationController(LythoAuthProperties lythoAuthProperties,
                                    ObjectMapper objectMapper) {
        this.lythoAuthProperties = lythoAuthProperties;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/initiate")
    public ResponseEntity<Void> initiate(HttpServletRequest request) {
        Map<String, Object> statePayload = buildStatePayload(request);
        String redirectUrl = UriComponentsBuilder
                .fromHttpUrl(buildAuthorizeUrl())
                .queryParam("client_id", lythoAuthProperties.getClientId())
                .queryParam("redirect_uri", lythoAuthProperties.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", lythoAuthProperties.getScope())
                .queryParam("state", encodeState(statePayload))
                .build(true)
                .toUriString();

        log.info("Redirecting Asana auth initiation to Lytho realm {} for Asana user {} in workspace {}",
                lythoAuthProperties.getRealm(),
                request.getParameter("user"),
                request.getParameter("workspace"));

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, redirectUrl);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    private String buildAuthorizeUrl() {
        String baseUrl = lythoAuthProperties.getKeycloakBaseUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/realms/" + lythoAuthProperties.getRealm() + "/protocol/openid-connect/auth";
    }

    private Map<String, Object> buildStatePayload(HttpServletRequest request) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("asanaClientId", lythoAuthProperties.getAsanaClientId());
        state.put("receivedAt", Instant.now().toString());

        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                queryParams.add(key, values[0]);
            }
        });
        state.put("query", queryParams);
        return state;
    }

    private String encodeState(Map<String, Object> statePayload) {
        try {
            String json = objectMapper.writeValueAsString(statePayload);
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize auth state", e);
        }
    }
}
