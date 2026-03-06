package com.baker.integration.asana.controller;

import com.baker.integration.asana.config.AsanaAppProperties;
import com.baker.integration.asana.model.asana.AsanaAttachment;
import com.baker.integration.asana.model.asana.AsanaCustomField;
import com.baker.integration.asana.model.asana.AsanaSubmitRequest;
import com.baker.integration.asana.model.asana.AsanaTag;
import com.baker.integration.asana.service.AsanaApiService;
import com.baker.integration.asana.service.AsanaSignatureVerificationService;
import com.baker.integration.asana.service.AsanaSubmitFlowService;
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
    private final AsanaAppProperties asanaAppProperties;
    private final AsanaApiService asanaApiService;
    private final AsanaSubmitFlowService asanaSubmitFlowService;

    public AsanaFormController(AsanaSignatureVerificationService signatureService,
                               AsanaAppProperties asanaAppProperties,
                               AsanaApiService asanaApiService,
                               AsanaSubmitFlowService asanaSubmitFlowService) {
        this.signatureService = signatureService;
        this.asanaAppProperties = asanaAppProperties;
        this.asanaApiService = asanaApiService;
        this.asanaSubmitFlowService = asanaSubmitFlowService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        log.info("Health check hit");
        return ResponseEntity.ok(Map.of("status", "ok", "version", "v3"));
    }

    @GetMapping("/widget")
    public ResponseEntity<Map<String, Object>> getWidget(
            @RequestParam(required = false) String task,
            @RequestParam(required = false) String user,
            @RequestParam(required = false, name = "resource_url") String resourceUrl,
            @RequestHeader("x-asana-request-signature") String signature,
            HttpServletRequest request) {

        String queryString = request.getQueryString() != null ? request.getQueryString() : "";
        signatureService.verifyGetRequest(queryString, signature);

        log.info("Widget requested for task: {}", task);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("title", "Lytho DAM");
        metadata.put("subtitle", "Lytho DAM Integration");
        metadata.put("fields", List.of());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("template", "summary_with_details_v0");
        response.put("metadata", metadata);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/form-metadata")
    public ResponseEntity<Map<String, Object>> getFormMetadata(
            @RequestParam String task,
            @RequestParam String user,
            @RequestParam String workspace,
            @RequestHeader("x-asana-request-signature") String signature,
            HttpServletRequest request) {

        String queryString = request.getQueryString() != null ? request.getQueryString() : "";
        signatureService.verifyGetRequest(queryString, signature);

        log.info("Form metadata requested for task: {}, user: {}", task, user);

        String accessToken = asanaAppProperties.getPersonalAccessToken();
        String userEmail = asanaApiService.getUserEmail(user, accessToken);
        List<AsanaAttachment> attachments = asanaApiService.getTaskAttachments(task, accessToken);
        List<AsanaTag> tags = asanaApiService.getTaskTags(task, accessToken);
        List<AsanaCustomField> customFields = asanaApiService.getTaskCustomFields(task, accessToken);

        String baseUrl = getBaseUrl(request);
        Map<String, Object> formResponse = buildFormMetadata(baseUrl, userEmail, attachments, tags, customFields);
        return ResponseEntity.ok(formResponse);
    }

    @PostMapping({"/on-submit", "/submit"})
    public ResponseEntity<Map<String, Object>> onSubmit(
            @RequestBody String rawBody,
            @RequestHeader("x-asana-request-signature") String signature) throws IOException {

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(rawBody);

        // Asana sends data as a JSON-encoded string and signs the unescaped string value
        String dataJson = root.get("data").asText();
        log.info("on-submit raw data string: {}", dataJson);

        signatureService.verifyPostRequest(dataJson, signature);

        AsanaSubmitRequest submitRequest = mapper.readValue(dataJson, AsanaSubmitRequest.class);

        log.info("on-submit parsed - task: {}, user: {}, workspace: {}, values: {}",
                submitRequest.getTask(), submitRequest.getUser(),
                submitRequest.getWorkspace(), submitRequest.getValues());

        List<String> selectedAttachments = extractSelectedAttachments(submitRequest.getValues());
        log.info("on-submit attachments selected - task: {}, user: {}, selectedAttachmentCount: {}, selectedAttachmentGids: {}",
                submitRequest.getTask(), submitRequest.getUser(), selectedAttachments.size(), selectedAttachments);

        if (selectedAttachments.isEmpty()) {
            log.info("on-submit no attachments selected - task: {}, user: {}",
                    submitRequest.getTask(), submitRequest.getUser());
            return ResponseEntity.ok(buildStatusResponse("no_attachments_selected", "No attachments selected"));
        }

        Map<String, Object> response = asanaSubmitFlowService.handleSubmit(
                submitRequest,
                selectedAttachments
        );
        log.info("on-submit accepted - task: {}, user: {}, response: {}",
                submitRequest.getTask(), submitRequest.getUser(), response);
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> buildFormMetadata(String baseUrl,
                                                    String userEmail,
                                                    List<AsanaAttachment> attachments,
                                                    List<AsanaTag> tags,
                                                    List<AsanaCustomField> customFields) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("title", "Send Attachments to Lytho");
        metadata.put("on_submit_callback", baseUrl + "/asana/submit");

        List<Map<String, Object>> fields = new ArrayList<>();

        // User email display
        if (userEmail != null && !userEmail.isBlank()) {
            Map<String, Object> emailField = new LinkedHashMap<>();
            emailField.put("type", "static_text");
            emailField.put("id", "user_email_info");
            emailField.put("name", "User: " + userEmail);
            fields.add(emailField);
        }

        // Custom fields section
        if (!customFields.isEmpty()) {
            List<AsanaCustomField> fieldsWithValues = customFields.stream()
                    .filter(cf -> cf.getDisplayValue() != null && !cf.getDisplayValue().isEmpty())
                    .toList();

            if (!fieldsWithValues.isEmpty()) {
                List<Map<String, String>> cfOptions = fieldsWithValues.stream()
                        .map(cf -> {
                            Map<String, String> option = new LinkedHashMap<>();
                            option.put("id", cf.getGid());
                            option.put("label", cf.getName() + ": " + cf.getDisplayValue());
                            return option;
                        })
                        .collect(Collectors.toList());

                Map<String, Object> cfField = new LinkedHashMap<>();
                cfField.put("type", "checkbox");
                cfField.put("id", "selected_custom_fields");
                cfField.put("name", "Select custom fields");
                cfField.put("is_required", false);
                cfField.put("options", cfOptions);
                fields.add(cfField);
            }
        }

        // Tags section
        if (!tags.isEmpty()) {
            List<Map<String, String>> tagOptions = tags.stream()
                    .map(t -> {
                        Map<String, String> option = new LinkedHashMap<>();
                        option.put("id", t.getGid());
                        option.put("label", t.getName());
                        return option;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> tagsField = new LinkedHashMap<>();
            tagsField.put("type", "checkbox");
            tagsField.put("id", "selected_tags");
            tagsField.put("name", "Select tags");
            tagsField.put("is_required", false);
            tagsField.put("options", tagOptions);
            fields.add(tagsField);
        }

        // Attachments section
        if (attachments.isEmpty()) {
            Map<String, Object> infoField = new LinkedHashMap<>();
            infoField.put("type", "static_text");
            infoField.put("id", "no_attachments_info");
            infoField.put("name", "This task has no downloadable attachments.");
            fields.add(infoField);
        } else {
            List<Map<String, String>> options = attachments.stream()
                    .map(a -> {
                        Map<String, String> option = new LinkedHashMap<>();
                        option.put("id", a.getGid());
                        String label = a.getName();
                        if (a.getContentType() != null) {
                            label += " [" + a.getContentType() + "]";
                        }
                        if (a.getSize() != null) {
                            label += " (" + formatFileSize(a.getSize()) + ")";
                        }
                        option.put("label", label);
                        return option;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> selectField = new LinkedHashMap<>();
            selectField.put("type", "checkbox");
            selectField.put("id", "selected_attachments");
            selectField.put("name", "Select attachments to send");
            selectField.put("is_required", true);
            selectField.put("options", options);
            fields.add(selectField);
        }

        metadata.put("fields", fields);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("template", "form_metadata_v0");
        response.put("metadata", metadata);
        return response;
    }

    private Map<String, Object> buildStatusResponse(String status, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", status);
        response.put("message", message);
        return response;
    }

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        if ((scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443)) {
            return scheme + "://" + host;
        }
        return scheme + "://" + host + ":" + port;
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
