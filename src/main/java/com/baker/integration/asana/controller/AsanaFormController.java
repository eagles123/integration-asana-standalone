package com.baker.integration.asana.controller;

import com.baker.integration.asana.config.AsanaAppProperties;
import com.baker.integration.asana.model.asana.AsanaAttachment;
import com.baker.integration.asana.model.asana.AsanaCustomField;
import com.baker.integration.asana.model.asana.AsanaSubmitRequest;
import com.baker.integration.asana.model.asana.AsanaTag;
import com.baker.integration.asana.service.AsanaApiService;
import com.baker.integration.asana.service.AsanaSignatureVerificationService;
import com.baker.integration.asana.service.AttachmentUploadOrchestrator;
import com.baker.integration.asana.service.DamTokenStore;
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
    private final DamTokenStore damTokenStore;
    private final AsanaApiService asanaApiService;
    private final AttachmentUploadOrchestrator uploadOrchestrator;

    public AsanaFormController(AsanaSignatureVerificationService signatureService,
                               AsanaAppProperties asanaAppProperties,
                               DamTokenStore damTokenStore,
                               AsanaApiService asanaApiService,
                               AttachmentUploadOrchestrator uploadOrchestrator) {
        this.signatureService = signatureService;
        this.asanaAppProperties = asanaAppProperties;
        this.damTokenStore = damTokenStore;
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
            @RequestHeader("x-asana-request-signature") String signature,
            HttpServletRequest request) {

        log.info("=== FORM-METADATA REQUEST RECEIVED ===");
        log.info("Request URL: {}", request.getRequestURL());
        log.info("Query String: {}", request.getQueryString());
        log.info("Signature Header: {}", signature);
        log.info("X-Forwarded-Proto: {}", request.getHeader("X-Forwarded-Proto"));
        log.info("X-Forwarded-Host: {}", request.getHeader("X-Forwarded-Host"));

        String queryString = request.getQueryString() != null ? request.getQueryString() : "";
        signatureService.verifyGetRequest(queryString, signature);

        log.info("Form metadata requested for task: {}, user: {}", task, user);

        String baseUrl = getBaseUrl(request);

        if (!damTokenStore.hasValidToken(user)) {
            return ResponseEntity.ok(buildTenantInputForm(baseUrl));
        }

        String accessToken = asanaAppProperties.getPat();
        List<AsanaAttachment> attachments = asanaApiService.getTaskAttachments(task, accessToken);
        List<AsanaTag> tags = asanaApiService.getTaskTags(task, accessToken);
        List<AsanaCustomField> customFields = asanaApiService.getTaskCustomFields(task, accessToken);

        Map<String, Object> formResponse = buildFormMetadata(baseUrl, attachments, tags, customFields);
        return ResponseEntity.ok(formResponse);
    }

    @PostMapping("/on-submit")
    public ResponseEntity<Map<String, Object>> onSubmit(
            @RequestBody String rawBody,
            @RequestHeader("x-asana-request-signature") String signature,
            HttpServletRequest request) throws IOException {

        log.info("POST raw body: {}", rawBody);

        // Asana signs the raw JSON value of the "data" field
        // Extract the exact raw substring to preserve original formatting
        String dataJson = extractDataField(rawBody);
        log.info("POST signature verification - data JSON: {}", dataJson);

        signatureService.verifyPostRequest(dataJson, signature);

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        AsanaSubmitRequest submitRequest = mapper.readValue(dataJson, AsanaSubmitRequest.class);

        log.info("Form submitted for task: {}, user: {}", submitRequest.getTask(), submitRequest.getUser());

        // Handle tenant login form submission
        String lythoTenant = extractStringValue(submitRequest.getValues(), "lytho_tenant");
        if (lythoTenant != null && !lythoTenant.isBlank()) {
            String state = damTokenStore.createLoginState(
                    submitRequest.getUser(), submitRequest.getWorkspace(), lythoTenant.trim());
            String baseUrl = getBaseUrl(request);
            String loginUrl = baseUrl + "/auth/lytho/login?state=" + state;
            return ResponseEntity.ok(buildLoginLinkForm(loginUrl));
        }

        if (!damTokenStore.hasValidToken(submitRequest.getUser())) {
            return ResponseEntity.ok(buildMessageResponse("Not Connected",
                    "Your Lytho DAM session has expired. Please close this dialog and re-open it to log in again."));
        }

        List<String> selectedAttachments = extractSelectedAttachments(submitRequest.getValues());

        if (selectedAttachments.isEmpty()) {
            return ResponseEntity.ok(buildMessageResponse("No Attachments Selected",
                    "No attachments were selected for upload."));
        }

        uploadOrchestrator.processAsync(
                submitRequest.getTask(),
                submitRequest.getUser(),
                submitRequest.getWorkspace(),
                selectedAttachments
        );

        return ResponseEntity.ok(buildMessageResponse("Upload Started",
                selectedAttachments.size() + " attachment(s) are being uploaded to Lytho. " +
                        "This may take a few minutes depending on file sizes."));
    }

    private Map<String, Object> buildFormMetadata(String baseUrl,
                                                    List<AsanaAttachment> attachments,
                                                    List<AsanaTag> tags,
                                                    List<AsanaCustomField> customFields) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("title", "Send Attachments to Lytho");
        metadata.put("on_submit_callback", baseUrl + "/asana/on-submit");

        List<Map<String, Object>> fields = new ArrayList<>();

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
            selectField.put("name", "Select attachments to upload");
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

    private Map<String, Object> buildTenantInputForm(String baseUrl) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("title", "Connect to Lytho DAM");
        metadata.put("on_submit_callback", baseUrl + "/asana/on-submit");

        List<Map<String, Object>> fields = new ArrayList<>();

        Map<String, Object> infoField = new LinkedHashMap<>();
        infoField.put("type", "static_text");
        infoField.put("id", "login_info");
        infoField.put("name", "Enter your Lytho tenant name to connect (e.g. \"qaorange\").");
        fields.add(infoField);

        Map<String, Object> tenantField = new LinkedHashMap<>();
        tenantField.put("type", "single_line_text");
        tenantField.put("id", "lytho_tenant");
        tenantField.put("name", "Lytho Tenant");
        tenantField.put("is_required", true);
        tenantField.put("placeholder", "your-tenant-name");
        fields.add(tenantField);

        metadata.put("fields", fields);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("template", "form_metadata_v0");
        response.put("metadata", metadata);
        return response;
    }

    private Map<String, Object> buildLoginLinkForm(String loginUrl) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("title", "Log in to Lytho");

        List<Map<String, Object>> fields = new ArrayList<>();

        Map<String, Object> infoField = new LinkedHashMap<>();
        infoField.put("type", "static_text");
        infoField.put("id", "login_link");
        infoField.put("name", "Open this link in a new tab to log in:\n\n" + loginUrl
                + "\n\nAfter logging in, close this dialog and re-open it.");
        fields.add(infoField);

        metadata.put("fields", fields);

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

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        if ((scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443)) {
            return scheme + "://" + host;
        }
        return scheme + "://" + host + ":" + port;
    }

    private String extractDataField(String rawBody) {
        // Find "data": in the raw body and extract the value exactly as-is
        int dataKeyIndex = rawBody.indexOf("\"data\"");
        if (dataKeyIndex == -1) return rawBody;
        int colonIndex = rawBody.indexOf(':', dataKeyIndex + 6);
        if (colonIndex == -1) return rawBody;

        // Skip whitespace after colon
        int valueStart = colonIndex + 1;
        while (valueStart < rawBody.length() && rawBody.charAt(valueStart) == ' ') {
            valueStart++;
        }

        // The value is everything from here to the end, minus the closing }
        // Body format: {"data":<value>}
        int valueEnd = rawBody.lastIndexOf('}');
        if (valueEnd <= valueStart) return rawBody;

        return rawBody.substring(valueStart, valueEnd);
    }

    private String extractStringValue(Map<String, Object> values, String key) {
        if (values == null) return null;
        Object value = values.get(key);
        return value != null ? value.toString() : null;
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
