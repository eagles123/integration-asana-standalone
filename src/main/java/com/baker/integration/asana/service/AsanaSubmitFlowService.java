package com.baker.integration.asana.service;

import com.baker.integration.asana.config.AsanaAppProperties;
import com.baker.integration.asana.model.asana.AsanaSubmitRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AsanaSubmitFlowService {

    private static final Logger log = LoggerFactory.getLogger(AsanaSubmitFlowService.class);

    private final AsanaAppProperties asanaAppProperties;
    private final AsanaApiService asanaApiService;
    private final AttachmentUploadOrchestrator attachmentUploadOrchestrator;

    public AsanaSubmitFlowService(AsanaAppProperties asanaAppProperties,
                                  AsanaApiService asanaApiService,
                                  AttachmentUploadOrchestrator attachmentUploadOrchestrator) {
        this.asanaAppProperties = asanaAppProperties;
        this.asanaApiService = asanaApiService;
        this.attachmentUploadOrchestrator = attachmentUploadOrchestrator;
    }

    public Map<String, Object> handleSubmit(AsanaSubmitRequest submitRequest,
                                            List<String> selectedAttachments) {
        String flowId = UUID.randomUUID().toString();
        log.info("submit flow started - flowId={}, task={}, user={}, workspace={}, selectedAttachmentCount={}",
                flowId, submitRequest.getTask(), submitRequest.getUser(),
                submitRequest.getWorkspace(), selectedAttachments.size());

        String accessToken = asanaAppProperties.getPersonalAccessToken();
        String userEmail = asanaApiService.getUserEmail(submitRequest.getUser(), accessToken);
        log.info("submit flow user resolved - flowId={}, user={}, emailFound={}",
                flowId, submitRequest.getUser(), userEmail != null && !userEmail.isBlank());

        attachmentUploadOrchestrator.processAsync(
                submitRequest.getTask(),
                submitRequest.getUser(),
                userEmail,
                selectedAttachments,
                flowId
        ).whenComplete((assets, error) -> {
            if (error != null) {
                log.error("async upload failed - flowId={}, task={}, error={}",
                        flowId, submitRequest.getTask(), error.getMessage(), error);
                return;
            }
            int successCount = assets != null ? assets.size() : 0;
            log.info("async upload finished - flowId={}, task={}, successCount={}, selectedAttachmentCount={}",
                    flowId, submitRequest.getTask(), successCount, selectedAttachments.size());
        });

        log.info("submit flow accepted - flowId={}, task={}", flowId, submitRequest.getTask());
        return buildSuccessResponse("Upload started for " + selectedAttachments.size() + " attachment(s)");
    }

    private Map<String, Object> buildSuccessResponse(String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("resource_name", message);
        response.put("resource_url", "https://app.asana.com");
        response.put("status", "ok");
        return response;
    }
}
