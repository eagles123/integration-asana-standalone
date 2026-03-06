package com.baker.integration.asana.service;

import com.baker.integration.asana.exception.FileTransferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.time.Duration;

@Service
public class FileTransferService {

    private static final Logger log = LoggerFactory.getLogger(FileTransferService.class);

    private final WebClient streamingWebClient;
    private final long timeoutMinutes;

    public FileTransferService(@Qualifier("streamingWebClient") WebClient streamingWebClient,
                               @Value("${file-transfer.timeout-minutes:10}") long timeoutMinutes) {
        this.streamingWebClient = streamingWebClient;
        this.timeoutMinutes = timeoutMinutes;
    }

    public void transferFile(String sourceUrl, String presignedUploadUrl, String contentType, Long contentLength,
                             String flowId, String attachmentGid) {
        try {
            long startedAt = System.currentTimeMillis();
            URI sourceUri = URI.create(sourceUrl);
            URI uploadUri = URI.create(presignedUploadUrl);
            log.info("file transfer started - flowId={}, attachmentGid={}, contentType={}, size={}, sourceHost={}, uploadHost={}",
                    flowId, attachmentGid, contentType, contentLength, sourceUri.getHost(), uploadUri.getHost());

            Flux<DataBuffer> downloadStream = streamingWebClient.get()
                    .uri(sourceUri)
                    .retrieve()
                    .bodyToFlux(DataBuffer.class);

            WebClient.RequestBodySpec uploadSpec = streamingWebClient.put()
                    .uri(uploadUri)
                    .header(HttpHeaders.CONTENT_TYPE, contentType);

            if (contentLength != null && contentLength > 0) {
                uploadSpec = uploadSpec.header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength));
            }

            uploadSpec
                    .body(BodyInserters.fromDataBuffers(downloadStream))
                    .retrieve()
                    .toBodilessEntity()
                    .block(Duration.ofMinutes(timeoutMinutes));

            long durationMs = System.currentTimeMillis() - startedAt;
            log.info("file transfer completed - flowId={}, attachmentGid={}, durationMs={}",
                    flowId, attachmentGid, durationMs);
        } catch (WebClientResponseException e) {
            log.error("file transfer failed - flowId={}, attachmentGid={}, status={}, body={}",
                    flowId, attachmentGid, e.getStatusCode(), truncate(e.getResponseBodyAsString()));
            throw new FileTransferException("File transfer failed", e);
        } catch (Exception e) {
            log.error("file transfer failed - flowId={}, attachmentGid={}, error={}",
                    flowId, attachmentGid, e.getMessage(), e);
            throw new FileTransferException("File transfer failed", e);
        }
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
