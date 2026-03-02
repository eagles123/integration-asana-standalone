package com.baker.integration.asana.service;

import com.baker.integration.asana.exception.AsanaApiException;
import com.baker.integration.asana.model.asana.AsanaAttachment;
import com.baker.integration.asana.model.asana.AsanaAttachmentDetailResponse;
import com.baker.integration.asana.model.asana.AsanaAttachmentListResponse;
import com.baker.integration.asana.model.asana.AsanaCustomField;
import com.baker.integration.asana.model.asana.AsanaTag;
import com.baker.integration.asana.model.asana.AsanaTagListResponse;
import com.baker.integration.asana.model.asana.AsanaTaskResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;

@Service
public class AsanaApiService {

    private static final Logger log = LoggerFactory.getLogger(AsanaApiService.class);
    private static final String OPT_FIELDS = "gid,name,download_url,content_type,size,host,view_url";

    private final WebClient asanaWebClient;

    public AsanaApiService(@Qualifier("asanaWebClient") WebClient asanaWebClient) {
        this.asanaWebClient = asanaWebClient;
    }

    public List<AsanaAttachment> getTaskAttachments(String taskGid, String accessToken) {
        try {
            AsanaAttachmentListResponse response = asanaWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/tasks/{taskGid}/attachments")
                            .queryParam("opt_fields", OPT_FIELDS)
                            .build(taskGid))
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(AsanaAttachmentListResponse.class)
                    .block();

            if (response == null || response.getData() == null) {
                log.warn("No attachments found for task: {}", taskGid);
                return Collections.emptyList();
            }

            List<AsanaAttachment> attachments = response.getData().stream()
                    .filter(a -> "asana".equals(a.getHost()))
                    .toList();

            log.info("Found {} downloadable attachments for task: {}", attachments.size(), taskGid);
            return attachments;
        } catch (Exception e) {
            throw new AsanaApiException("Failed to fetch attachments for task: " + taskGid, e);
        }
    }

    public List<AsanaTag> getTaskTags(String taskGid, String accessToken) {
        try {
            AsanaTagListResponse response = asanaWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/tasks/{taskGid}/tags")
                            .queryParam("opt_fields", "gid,name,color")
                            .build(taskGid))
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(AsanaTagListResponse.class)
                    .block();

            if (response == null || response.getData() == null) {
                log.warn("No tags found for task: {}", taskGid);
                return Collections.emptyList();
            }

            log.info("Found {} tags for task: {}", response.getData().size(), taskGid);
            return response.getData();
        } catch (Exception e) {
            log.warn("Failed to fetch tags for task: {} - {}", taskGid, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<AsanaCustomField> getTaskCustomFields(String taskGid, String accessToken) {
        try {
            AsanaTaskResponse response = asanaWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/tasks/{taskGid}")
                            .queryParam("opt_fields", "custom_fields,custom_fields.name,custom_fields.type,custom_fields.display_value,custom_fields.enum_value,custom_fields.enum_value.name,custom_fields.multi_enum_values,custom_fields.multi_enum_values.name,custom_fields.text_value,custom_fields.number_value")
                            .build(taskGid))
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(AsanaTaskResponse.class)
                    .block();

            if (response == null || response.getData() == null
                    || response.getData().getCustomFields() == null) {
                log.warn("No custom fields found for task: {}", taskGid);
                return Collections.emptyList();
            }

            List<AsanaCustomField> fields = response.getData().getCustomFields();
            log.info("Found {} custom fields for task: {}", fields.size(), taskGid);
            return fields;
        } catch (Exception e) {
            log.warn("Failed to fetch custom fields for task: {} - {}", taskGid, e.getMessage());
            return Collections.emptyList();
        }
    }

    public AsanaAttachment getAttachmentDetail(String attachmentGid, String accessToken) {
        try {
            AsanaAttachmentDetailResponse response = asanaWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/attachments/{attachmentGid}")
                            .queryParam("opt_fields", OPT_FIELDS)
                            .build(attachmentGid))
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(AsanaAttachmentDetailResponse.class)
                    .block();

            if (response == null || response.getData() == null) {
                throw new AsanaApiException("Attachment not found: " + attachmentGid);
            }

            return response.getData();
        } catch (AsanaApiException e) {
            throw e;
        } catch (Exception e) {
            throw new AsanaApiException("Failed to fetch attachment detail: " + attachmentGid, e);
        }
    }
}
