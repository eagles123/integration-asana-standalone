package com.baker.integration.asana.service;

import com.baker.integration.asana.config.ParagonProperties;
import com.baker.integration.asana.exception.AsanaApiException;
import com.baker.integration.asana.model.paragon.ParagonTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class ParagonTokenService {

    private static final Logger log = LoggerFactory.getLogger(ParagonTokenService.class);

    private final WebClient paragonWebClient;
    private final ParagonProperties paragonProperties;

    public ParagonTokenService(@Qualifier("paragonWebClient") WebClient paragonWebClient,
                               ParagonProperties paragonProperties) {
        this.paragonWebClient = paragonWebClient;
        this.paragonProperties = paragonProperties;
    }

    public String getAsanaToken(String userId) {
        try {
            ParagonTokenResponse response = paragonWebClient.get()
                    .uri("/projects/{projectId}/sdk/proxy/asana/credentials", paragonProperties.getProjectId())
                    .header("Authorization", "Bearer " + paragonProperties.getSigningKey())
                    .header("X-Paragon-User-Id", userId)
                    .retrieve()
                    .bodyToMono(ParagonTokenResponse.class)
                    .block();

            if (response == null || response.getAccessToken() == null) {
                throw new AsanaApiException("No Asana token returned from Paragon for user: " + userId);
            }

            log.debug("Retrieved Asana token from Paragon for user: {}", userId);
            return response.getAccessToken();
        } catch (AsanaApiException e) {
            throw e;
        } catch (Exception e) {
            throw new AsanaApiException("Failed to retrieve Asana token from Paragon for user: " + userId, e);
        }
    }
}
