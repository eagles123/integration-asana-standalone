package com.baker.integration.asana.service;

import com.baker.integration.asana.config.LinkFlowProperties;
import com.baker.integration.asana.model.connection.LinkStatePayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Service
public class StateTokenService {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final LinkFlowProperties linkFlowProperties;
    private final ObjectMapper objectMapper;

    public StateTokenService(LinkFlowProperties linkFlowProperties, ObjectMapper objectMapper) {
        this.linkFlowProperties = linkFlowProperties;
        this.objectMapper = objectMapper;
    }

    public String sign(LinkStatePayload payload) {
        validateSigningSecret();
        try {
            String json = objectMapper.writeValueAsString(payload);
            String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(json.getBytes(StandardCharsets.UTF_8));
            String signature = hmac(encodedPayload);
            return encodedPayload + "." + signature;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign link state payload", e);
        }
    }

    public LinkStatePayload verifyAndParse(String signedState) {
        validateSigningSecret();

        if (signedState == null || signedState.isBlank() || !signedState.contains(".")) {
            throw new IllegalArgumentException("State token is missing or malformed");
        }

        String[] parts = signedState.split("\\.", 2);
        String encodedPayload = parts[0];
        String actualSignature = parts[1];

        String expectedSignature = hmac(encodedPayload);
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                actualSignature.getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("Invalid state token signature");
        }

        try {
            byte[] decodedPayload = Base64.getUrlDecoder().decode(encodedPayload);
            LinkStatePayload payload = objectMapper.readValue(decodedPayload, LinkStatePayload.class);
            if (payload.getExpEpochSeconds() < Instant.now().getEpochSecond()) {
                throw new IllegalArgumentException("State token expired");
            }
            return payload;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse state token payload", e);
        }
    }

    private String hmac(String content) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(
                    linkFlowProperties.getStateSigningSecret().getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256));
            byte[] digest = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign state token", e);
        }
    }

    private void validateSigningSecret() {
        String secret = linkFlowProperties.getStateSigningSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("link-flow.state-signing-secret must be configured");
        }
    }
}
