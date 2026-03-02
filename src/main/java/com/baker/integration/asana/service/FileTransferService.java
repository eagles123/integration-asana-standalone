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
import reactor.core.publisher.Flux;

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

    public void transferFile(String sourceUrl, String presignedUploadUrl, String contentType, Long contentLength) {
        try {
            log.info("Starting file transfer: contentType={}, size={}", contentType, contentLength);

            Flux<DataBuffer> downloadStream = streamingWebClient.get()
                    .uri(sourceUrl)
                    .retrieve()
                    .bodyToFlux(DataBuffer.class);

            WebClient.RequestBodySpec uploadSpec = streamingWebClient.put()
                    .uri(presignedUploadUrl)
                    .header(HttpHeaders.CONTENT_TYPE, contentType);

            if (contentLength != null && contentLength > 0) {
                uploadSpec = uploadSpec.header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength));
            }

            uploadSpec
                    .body(BodyInserters.fromDataBuffers(downloadStream))
                    .retrieve()
                    .toBodilessEntity()
                    .block(Duration.ofMinutes(timeoutMinutes));

            log.info("File transfer completed successfully");
        } catch (Exception e) {
            throw new FileTransferException("File transfer failed", e);
        }
    }
}
