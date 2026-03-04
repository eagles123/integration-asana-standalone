package com.baker.integration.asana.service;

import com.baker.integration.asana.config.AsanaAppProperties;
import com.baker.integration.asana.model.asana.AsanaAttachment;
import com.baker.integration.asana.model.assets.AssetResponse;
import com.baker.integration.asana.model.assets.UploadLinkResponse;
import com.baker.integration.asana.model.integration.IntegrationIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class AttachmentUploadOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AttachmentUploadOrchestrator.class);

    private final AsanaAppProperties asanaAppProperties;
    private final DamAuthService damAuthService;
    private final IntegrationIdentityService integrationIdentityService;
    private final AsanaApiService asanaApiService;
    private final ServiceAssetsClient serviceAssetsClient;
    private final FileTransferService fileTransferService;

    public AttachmentUploadOrchestrator(AsanaAppProperties asanaAppProperties,
                                       DamAuthService damAuthService,
                                       IntegrationIdentityService integrationIdentityService,
                                       AsanaApiService asanaApiService,
                                       ServiceAssetsClient serviceAssetsClient,
                                       FileTransferService fileTransferService) {
        this.asanaAppProperties = asanaAppProperties;
        this.damAuthService = damAuthService;
        this.integrationIdentityService = integrationIdentityService;
        this.asanaApiService = asanaApiService;
        this.serviceAssetsClient = serviceAssetsClient;
        this.fileTransferService = fileTransferService;
    }

    @Async("fileTransferExecutor")
    public CompletableFuture<List<AssetResponse>> processAsync(String taskGid, String asanaUserId,
                                                                String asanaWorkspaceId,
                                                                List<String> attachmentGids) {
        IntegrationIdentity identity = integrationIdentityService.resolveIdentity(asanaUserId, asanaWorkspaceId);
        log.info("Starting async upload of {} attachments from task: {}, tenant: {}, damUser: {}",
                attachmentGids.size(), taskGid, identity.getTenantId(), identity.getDamUserId());

        String asanaPat = asanaAppProperties.getPat();
        String damAccessToken = damAuthService.getValidAccessToken(asanaUserId);
        List<AssetResponse> results = new ArrayList<>();

        for (String attachmentGid : attachmentGids) {
            try {
                AsanaAttachment attachment = asanaApiService.getAttachmentDetail(attachmentGid, asanaPat);
                log.info("Processing attachment: {} ({})", attachment.getName(), attachmentGid);

                UploadLinkResponse uploadLink = serviceAssetsClient.getUploadLink(
                        attachment.getName(), attachment.getContentType(), identity, damAccessToken);

                fileTransferService.transferFile(
                        attachment.getDownloadUrl(),
                        uploadLink.getUploadLink(),
                        attachment.getContentType(),
                        attachment.getSize());

                AssetResponse asset = serviceAssetsClient.finalizeUpload(
                        uploadLink.getObjectKey(), identity, damAccessToken);
                results.add(asset);

                log.info("Successfully uploaded attachment {} as asset {}", attachmentGid, asset.getId());
            } catch (Exception e) {
                log.error("Failed to upload attachment {}: {}", attachmentGid, e.getMessage(), e);
            }
        }

        log.info("Upload batch completed: {}/{} attachments succeeded for task: {}",
                results.size(), attachmentGids.size(), taskGid);
        return CompletableFuture.completedFuture(results);
    }
}
