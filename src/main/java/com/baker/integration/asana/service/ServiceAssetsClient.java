package com.baker.integration.asana.service;

import com.baker.integration.asana.exception.FileTransferException;
import com.baker.integration.asana.model.assets.AssetResponse;
import com.baker.integration.asana.model.assets.UploadCompletedRequest;
import com.baker.integration.asana.model.assets.UploadLinkRequest;
import com.baker.integration.asana.model.assets.UploadLinkResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class ServiceAssetsClient {

    private static final Logger log = LoggerFactory.getLogger(ServiceAssetsClient.class);

    private final WebClient serviceAssetsWebClient;

    public ServiceAssetsClient(@Qualifier("serviceAssetsWebClient") WebClient serviceAssetsWebClient) {
        this.serviceAssetsWebClient = serviceAssetsWebClient;
    }

    public UploadLinkResponse getUploadLink(String fileName, String contentType, String userEmail) {
        log.info("Requesting upload link - fileName: {}, contentType: {}, userEmail: {}", fileName, contentType, userEmail);
        try {
            UploadLinkResponse response = serviceAssetsWebClient.put()
                    .uri("/integration/assets/upload/link")
                    .headers(headers -> {
                        if (userEmail != null) {
                            headers.set("X-User-Email", userEmail);
                        }
                    })
                    .bodyValue(new UploadLinkRequest(fileName, contentType))
                    .retrieve()
                    .bodyToMono(UploadLinkResponse.class)
                    .block();

            if (response == null || response.getUploadLink() == null) {
                throw new FileTransferException("No upload link returned for file: " + fileName);
            }

            log.debug("Obtained upload link for file: {}, objectKey: {}", fileName, response.getObjectKey());
            return response;
        } catch (FileTransferException e) {
            throw e;
        } catch (Exception e) {
            throw new FileTransferException("Failed to get upload link for file: " + fileName, e);
        }
    }

    public AssetResponse finalizeUpload(String objectKey, String userEmail) {
        log.info("Finalizing upload - objectKey: {}, userEmail: {}", objectKey, userEmail);
        try {
            AssetResponse response = serviceAssetsWebClient.post()
                    .uri("/integration/assets/upload/link/upload-completed")
                    .headers(headers -> {
                        if (userEmail != null) {
                            headers.set("X-User-Email", userEmail);
                        }
                    })
                    .bodyValue(new UploadCompletedRequest(objectKey))
                    .retrieve()
                    .bodyToMono(AssetResponse.class)
                    .block();

            if (response == null) {
                throw new FileTransferException("No response when finalizing upload for key: " + objectKey);
            }

            log.info("Upload finalized - asset ID: {}, fileName: {}", response.getId(), response.getFileName());
            return response;
        } catch (FileTransferException e) {
            throw e;
        } catch (Exception e) {
            throw new FileTransferException("Failed to finalize upload for key: " + objectKey, e);
        }
    }
}
