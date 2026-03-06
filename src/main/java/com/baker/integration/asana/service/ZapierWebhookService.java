package com.baker.integration.asana.service;

import com.baker.integration.asana.config.AsanaAppProperties;
import com.baker.integration.asana.config.ZapierProperties;
import com.baker.integration.asana.model.asana.AsanaAttachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class ZapierWebhookService {

    private static final Logger log = LoggerFactory.getLogger(ZapierWebhookService.class);

    private final ZapierProperties zapierProperties;
    private final AsanaAppProperties asanaAppProperties;
    private final AsanaApiService asanaApiService;
    private final WebClient webClient;

    public ZapierWebhookService(ZapierProperties zapierProperties,
                                AsanaAppProperties asanaAppProperties,
                                AsanaApiService asanaApiService) {
        this.zapierProperties = zapierProperties;
        this.asanaAppProperties = asanaAppProperties;
        this.asanaApiService = asanaApiService;
        this.webClient = WebClient.builder().build();
    }

    public boolean isConfigured() {
        return zapierProperties.getCatchHookUrl() != null
                && !zapierProperties.getCatchHookUrl().isBlank();
    }

    @Async("fileTransferExecutor")
    public CompletableFuture<Integer> dispatchSelectedAttachments(String taskGid,
                                                                  String asanaUserId,
                                                                  String workspaceGid,
                                                                  String userEmail,
                                                                  List<String> attachmentGids,
                                                                  Map<String, Object> values,
                                                                  String keycloakUserId) {
        log.info("Dispatching {} attachment(s) to Zapier for task: {}", attachmentGids.size(), taskGid);

        String accessToken = asanaAppProperties.getPersonalAccessToken();
        int sentCount = 0;
        List<String> selectedTags = extractList(values, "selected_tags");
        List<String> selectedCustomFields = extractList(values, "selected_custom_fields");

        for (String attachmentGid : attachmentGids) {
            try {
                AsanaAttachment attachment = asanaApiService.getAttachmentDetail(attachmentGid, accessToken);
                if (attachment.getDownloadUrl() == null || attachment.getDownloadUrl().isBlank()) {
                    log.warn("Skipping attachment {} because download_url is empty", attachmentGid);
                    continue;
                }
                String fileName = attachment.getName() != null && !attachment.getName().isBlank()
                        ? attachment.getName()
                        : "attachment-" + attachmentGid;

                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("event_type", "asana_modal_attachment_selected");
                payload.put("submitted_at", Instant.now().toString());
                payload.put("task_gid", taskGid);
                payload.put("workspace_gid", workspaceGid);
                payload.put("asana_user_gid", asanaUserId);
                payload.put("asana_user_email", userEmail);
                payload.put("keycloak_user_id", keycloakUserId);
                payload.put("attachment_gid", attachmentGid);
                payload.put("file_name", fileName);
                payload.put("file_url", attachment.getDownloadUrl());
                payload.put("content_type", attachment.getContentType());
                payload.put("size", attachment.getSize());
                payload.put("selected_tags", selectedTags);
                payload.put("selected_custom_fields", selectedCustomFields);

                webClient.post()
                        .uri(zapierProperties.getCatchHookUrl())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(payload)
                        .retrieve()
                        .toBodilessEntity()
                        .block(Duration.ofSeconds(zapierProperties.getTimeoutSeconds()));

                sentCount++;
                log.info("Dispatched attachment {} ({}) to Zapier", fileName, attachmentGid);
            } catch (Exception e) {
                log.error("Failed to dispatch attachment {} to Zapier: {}", attachmentGid, e.getMessage(), e);
            }
        }

        log.info("Zapier dispatch completed for task {}: {}/{} sent",
                taskGid, sentCount, attachmentGids.size());
        return CompletableFuture.completedFuture(sentCount);
    }

    @SuppressWarnings("unchecked")
    private List<String> extractList(Map<String, Object> values, String key) {
        if (values == null) {
            return Collections.emptyList();
        }
        Object rawValue = values.get(key);
        if (!(rawValue instanceof List<?> listValue)) {
            return Collections.emptyList();
        }
        return listValue.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.toList());
    }
}
