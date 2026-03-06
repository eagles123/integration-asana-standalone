package com.baker.integration.asana.service;

import com.baker.integration.asana.config.LinkFlowProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class TokenEncryptionService {

    private static final String CIPHER_ALGO = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private final LinkFlowProperties linkFlowProperties;
    private final SecureRandom secureRandom;

    public TokenEncryptionService(LinkFlowProperties linkFlowProperties) {
        this.linkFlowProperties = linkFlowProperties;
        this.secureRandom = new SecureRandom();
    }

    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKeySpec(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] result = ByteBuffer.allocate(iv.length + cipherText.length)
                    .put(iv)
                    .put(cipherText)
                    .array();
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt token", e);
        }
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null) {
            return null;
        }
        try {
            byte[] payload = Base64.getDecoder().decode(encryptedText);
            if (payload.length <= IV_BYTES) {
                throw new IllegalStateException("Encrypted payload is invalid");
            }
            byte[] iv = new byte[IV_BYTES];
            byte[] cipherText = new byte[payload.length - IV_BYTES];
            System.arraycopy(payload, 0, iv, 0, IV_BYTES);
            System.arraycopy(payload, IV_BYTES, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKeySpec(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plainBytes = cipher.doFinal(cipherText);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt token", e);
        }
    }

    private SecretKeySpec getSecretKeySpec() {
        String rawKey = linkFlowProperties.getTokenEncryptionKey();
        if (rawKey == null || rawKey.isBlank()) {
            throw new IllegalStateException("link-flow.token-encryption-key must be configured");
        }

        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(rawKey);
        } catch (Exception e) {
            throw new IllegalStateException("link-flow.token-encryption-key must be valid Base64", e);
        }

        if (decoded.length != 16 && decoded.length != 24 && decoded.length != 32) {
            throw new IllegalStateException("link-flow.token-encryption-key must decode to 16/24/32 bytes");
        }
        return new SecretKeySpec(decoded, "AES");
    }
}
