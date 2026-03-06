package com.baker.integration.asana.service;

import com.baker.integration.asana.config.ServiceAssetsProperties;
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
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class ServiceAssetsClient {

    private static final Logger log = LoggerFactory.getLogger(ServiceAssetsClient.class);
    private static final String API_KEY_HEADER = "x-api-key";

    private final WebClient serviceAssetsWebClient;
    private final ServiceAssetsProperties serviceAssetsProperties;

    public ServiceAssetsClient(@Qualifier("serviceAssetsWebClient") WebClient serviceAssetsWebClient,
                               ServiceAssetsProperties serviceAssetsProperties) {
        this.serviceAssetsWebClient = serviceAssetsWebClient;
        this.serviceAssetsProperties = serviceAssetsProperties;
    }

    public UploadLinkResponse getUploadLink(String fileName, String contentType,
                                            String flowId, String attachmentGid) {
        log.info("upload-link request started - flowId={}, attachmentGid={}, fileName={}, contentType={}",
                flowId, attachmentGid, fileName, contentType);
        try {
            UploadLinkResponse response = serviceAssetsWebClient.put()
                    .uri("/upload/link")
                    .header(API_KEY_HEADER, requireApiKey())
                    .bodyValue(new UploadLinkRequest(fileName, contentType))
                    .retrieve()
                    .bodyToMono(UploadLinkResponse.class)
                    .block();

            if (response == null || response.getUploadLink() == null) {
                throw new FileTransferException("No upload link returned for file: " + fileName);
            }

            log.info("upload-link request completed - flowId={}, attachmentGid={}, objectKey={}, exp={}",
                    flowId, attachmentGid, response.getObjectKey(), response.getExp());
            return response;
        } catch (FileTransferException e) {
            throw e;
        } catch (WebClientResponseException e) {
            log.error("upload-link request failed - flowId={}, attachmentGid={}, status={}, body={}",
                    flowId, attachmentGid, e.getStatusCode(), truncate(e.getResponseBodyAsString()));
            throw new FileTransferException("Failed to get upload link for file: " + fileName, e);
        } catch (Exception e) {
            log.error("upload-link request failed - flowId={}, attachmentGid={}, error={}",
                    flowId, attachmentGid, e.getMessage(), e);
            throw new FileTransferException("Failed to get upload link for file: " + fileName, e);
        }
    }

    public AssetResponse finalizeUpload(String objectKey, String flowId, String attachmentGid) {
        log.info("upload finalize started - flowId={}, attachmentGid={}, objectKey={}",
                flowId, attachmentGid, objectKey);
        try {
            AssetResponse response = serviceAssetsWebClient.post()
                    .uri("/upload/link/upload-completed")
                    .header(API_KEY_HEADER, requireApiKey())
                    .bodyValue(new UploadCompletedRequest(objectKey))
                    .retrieve()
                    .bodyToMono(AssetResponse.class)
                    .block();

            if (response == null) {
                throw new FileTransferException("No response when finalizing upload for key: " + objectKey);
            }

            log.info("upload finalize completed - flowId={}, attachmentGid={}, objectKey={}, assetId={}, fileName={}",
                    flowId, attachmentGid, objectKey, response.getId(), response.getFileName());
            return response;
        } catch (FileTransferException e) {
            throw e;
        } catch (WebClientResponseException e) {
            log.error("upload finalize failed - flowId={}, attachmentGid={}, objectKey={}, status={}, body={}",
                    flowId, attachmentGid, objectKey, e.getStatusCode(), truncate(e.getResponseBodyAsString()));
            throw new FileTransferException("Failed to finalize upload for key: " + objectKey, e);
        } catch (Exception e) {
            log.error("upload finalize failed - flowId={}, attachmentGid={}, objectKey={}, error={}",
                    flowId, attachmentGid, objectKey, e.getMessage(), e);
            throw new FileTransferException("Failed to finalize upload for key: " + objectKey, e);
        }
    }

    private String requireApiKey() {
        String apiKey = serviceAssetsProperties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("service-assets.api-key is not configured");
        }
        return apiKey;
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        int max = 500;
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max) + "...";
    }
}
