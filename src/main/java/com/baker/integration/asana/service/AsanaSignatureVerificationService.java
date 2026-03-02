package com.baker.integration.asana.service;

import com.baker.integration.asana.config.AsanaAppProperties;
import com.baker.integration.asana.exception.AsanaSignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class AsanaSignatureVerificationService {

    private static final Logger log = LoggerFactory.getLogger(AsanaSignatureVerificationService.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final AsanaAppProperties asanaAppProperties;

    public AsanaSignatureVerificationService(AsanaAppProperties asanaAppProperties) {
        this.asanaAppProperties = asanaAppProperties;
    }

    public void verifyGetRequest(String fullUrl, String signatureHeader) {
        log.info("Signature verification - Full URL: {}", fullUrl);
        log.info("Signature verification - Client secret length: {}",
                asanaAppProperties.getClientSecret() != null ? asanaAppProperties.getClientSecret().length() : "null");
        String computed = computeHmac(fullUrl);
        log.info("Signature verification - Computed: {}", computed);
        log.info("Signature verification - Received: {}", signatureHeader);
        if (!MessageDigest.isEqual(computed.getBytes(StandardCharsets.UTF_8),
                signatureHeader.getBytes(StandardCharsets.UTF_8))) {
            throw new AsanaSignatureException("Invalid signature for GET request");
        }
        log.info("GET request signature verified successfully");
    }

    public void verifyPostRequest(String requestBody, String signatureHeader) {
        String computed = computeHmac(requestBody);
        if (!MessageDigest.isEqual(computed.getBytes(StandardCharsets.UTF_8),
                signatureHeader.getBytes(StandardCharsets.UTF_8))) {
            throw new AsanaSignatureException("Invalid signature for POST request");
        }
        log.debug("POST request signature verified successfully");
    }

    private String computeHmac(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(
                    asanaAppProperties.getClientSecret().getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256
            );
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmacBytes);
        } catch (Exception e) {
            throw new AsanaSignatureException("Failed to compute HMAC signature", e);
        }
    }
}
