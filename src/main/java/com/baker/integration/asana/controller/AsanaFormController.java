package com.baker.integration.asana.controller;

import com.baker.integration.asana.config.AsanaAppProperties;
import com.baker.integration.asana.model.asana.AsanaAttachment;
import com.baker.integration.asana.model.asana.AsanaSubmitRequest;
import com.baker.integration.asana.service.AsanaApiService;
import com.baker.integration.asana.service.AsanaSignatureVerificationService;
import com.baker.integration.asana.service.AttachmentUploadOrchestrator;
// import com.baker.integration.asana.service.ParagonTokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/asana")
public class AsanaFormController {

    private static final Logger log = LoggerFactory.getLogger(AsanaFormController.class);

    private final AsanaSignatureVerificationService signatureService;
    // private final ParagonTokenService paragonTokenService;
    private final AsanaAppProperties asanaAppProperties;
    private final AsanaApiService asanaApiService;
    private final AttachmentUploadOrchestrator uploadOrchestrator;

    public AsanaFormController(AsanaSignatureVerificationService signatureService,
                               // ParagonTokenService paragonTokenService,
                               AsanaAppProperties asanaAppProperties,
                               AsanaApiService asanaApiService,
                               AttachmentUploadOrchestrator uploadOrchestrator) {
        this.signatureService = signatureService;
        // this.paragonTokenService = paragonTokenService;
        this.asanaAppProperties = asanaAppProperties;
        this.asanaApiService = asanaApiService;
        this.uploadOrchestrator = uploadOrchestrator;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        log.info("Health check hit");
        return ResponseEntity.ok(Map.of("status", "ok", "version", "debug-v2"));
    }

    @GetMapping("/form-metadata")
    public ResponseEntity<Map<String, Object>> getFormMetadata(
            @RequestParam String task,
            @RequestParam String user,
            @RequestParam String workspace,
            @RequestHeader(value = "x-asana-request-signature", required = false) String signature,
            HttpServletRequest request) {

        log.info("=== FORM-METADATA REQUEST RECEIVED ===");
        log.info("Request URL: {}", request.getRequestURL());
        log.info("Query String: {}", request.getQueryString());
        log.info("Signature Header: {}", signature);
        log.info("X-Forwarded-Proto: {}", request.getHeader("X-Forwarded-Proto"));
        log.info("X-Forwarded-Host: {}", request.getHeader("X-Forwarded-Host"));

        // TEMPORARY: Skip signature verification for debugging
        if (signature != null) {
            String fullUrl = request.getRequestURL().toString();
            if (request.getQueryString() != null) {
                fullUrl += "?" + request.getQueryString();
            }
            try {
                signatureService.verifyGetRequest(fullUrl, signature);
            } catch (Exception e) {
                log.warn("Signature verification failed (bypassed for debugging): {}", e.getMessage());
            }
        } else {
            log.warn("No signature header present");
        }

        log.info("Form metadata requested for task: {}, user: {}", task, user);

        // Use PAT directly instead of Paragon OAuth
        // String accessToken = paragonTokenService.getAsanaToken(user);
        String accessToken = asanaAppProperties.getPersonalAccessToken();
        List<AsanaAttachment> attachments = asanaApiService.getTaskAttachments(task, accessToken);

        Map<String, Object> formResponse = buildFormMetadata(attachments);
        return ResponseEntity.ok(formResponse);
    }

    @PostMapping("/on-submit")
    public ResponseEntity<Map<String, Object>> onSubmit(
            @RequestBody String rawBody,
            @RequestHeader("x-asana-request-signature") String signature,
            HttpServletRequest request) throws IOException {

        signatureService.verifyPostRequest(rawBody, signature);

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        AsanaSubmitRequest submitRequest = mapper.readValue(rawBody, AsanaSubmitRequest.class);

        log.info("Form submitted for task: {}, user: {}", submitRequest.getTask(), submitRequest.getUser());

        List<String> selectedAttachments = extractSelectedAttachments(submitRequest.getValues());

        if (selectedAttachments.isEmpty()) {
            return ResponseEntity.ok(buildMessageResponse("No Attachments Selected",
                    "No attachments were selected for upload."));
        }

        uploadOrchestrator.processAsync(
                submitRequest.getTask(),
                submitRequest.getUser(),
                selectedAttachments
        );

        return ResponseEntity.ok(buildMessageResponse("Upload Started",
                selectedAttachments.size() + " attachment(s) are being uploaded to Lytho. " +
                        "This may take a few minutes depending on file sizes."));
    }

    private Map<String, Object> buildFormMetadata(List<AsanaAttachment> attachments) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("title", "Send Attachments to Lytho");
        metadata.put("submit_button_text", "Upload to Lytho");
        metadata.put("on_submit_callback", "/asana/on-submit");

        if (attachments.isEmpty()) {
            Map<String, Object> infoField = new LinkedHashMap<>();
            infoField.put("type", "static_text");
            infoField.put("id", "no_attachments_info");
            infoField.put("name", "This task has no downloadable attachments.");
            metadata.put("fields", List.of(infoField));
        } else {
            List<Map<String, String>> options = attachments.stream()
                    .map(a -> {
                        Map<String, String> option = new LinkedHashMap<>();
                        option.put("id", a.getGid());
                        String label = a.getName();
                        if (a.getSize() != null) {
                            label += " (" + formatFileSize(a.getSize()) + ")";
                        }
                        option.put("label", label);
                        return option;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> selectField = new LinkedHashMap<>();
            selectField.put("type", "multi_enum");
            selectField.put("id", "selected_attachments");
            selectField.put("name", "Select attachments to upload");
            selectField.put("is_required", true);
            selectField.put("options", options);

            metadata.put("fields", List.of(selectField));
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("template", "form_metadata_v0");
        response.put("metadata", metadata);
        return response;
    }

    private Map<String, Object> buildMessageResponse(String title, String message) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("type", "static_text");
        field.put("id", "confirmation");
        field.put("name", message);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("title", title);
        metadata.put("fields", List.of(field));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("template", "form_metadata_v0");
        response.put("metadata", metadata);
        return response;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractSelectedAttachments(Map<String, Object> values) {
        if (values == null) return Collections.emptyList();
        Object selected = values.get("selected_attachments");
        if (selected instanceof List) {
            return ((List<Object>) selected).stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
