package com.baker.integration.asana.service;

import com.baker.integration.asana.config.AsanaAppProperties;
import com.baker.integration.asana.model.asana.AsanaAttachment;
import com.baker.integration.asana.model.assets.AssetResponse;
import com.baker.integration.asana.model.assets.UploadLinkResponse;
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
    private final AsanaApiService asanaApiService;
    private final ServiceAssetsClient serviceAssetsClient;
    private final FileTransferService fileTransferService;

    public AttachmentUploadOrchestrator(AsanaAppProperties asanaAppProperties,
                                       AsanaApiService asanaApiService,
                                       ServiceAssetsClient serviceAssetsClient,
                                       FileTransferService fileTransferService) {
        this.asanaAppProperties = asanaAppProperties;
        this.asanaApiService = asanaApiService;
        this.serviceAssetsClient = serviceAssetsClient;
        this.fileTransferService = fileTransferService;
    }

    @Async("fileTransferExecutor")
    public CompletableFuture<List<AssetResponse>> processAsync(String taskGid,
                                                                String asanaUserId,
                                                                String userEmail,
                                                                List<String> attachmentGids,
                                                                String flowId) {
        log.info("upload batch started - flowId={}, task={}, user={}, email={}, attachmentCount={}, attachmentGids={}",
                flowId, taskGid, asanaUserId, userEmail, attachmentGids.size(), attachmentGids);

        String accessToken = asanaAppProperties.getPersonalAccessToken();
        List<AssetResponse> results = new ArrayList<>();

        for (int i = 0; i < attachmentGids.size(); i++) {
            String attachmentGid = attachmentGids.get(i);
            try {
                log.info("attachment processing started - flowId={}, task={}, attachmentIndex={}, attachmentCount={}, attachmentGid={}",
                        flowId, taskGid, i + 1, attachmentGids.size(), attachmentGid);
                AsanaAttachment attachment = asanaApiService.getAttachmentDetail(attachmentGid, accessToken);
                String fileName = attachment.getName() != null && !attachment.getName().isBlank()
                        ? attachment.getName()
                        : "attachment-" + attachmentGid;
                String contentType = attachment.getContentType() != null && !attachment.getContentType().isBlank()
                        ? attachment.getContentType()
                        : "application/octet-stream";

                if (attachment.getDownloadUrl() == null || attachment.getDownloadUrl().isBlank()) {
                    log.warn("attachment skipped - flowId={}, task={}, attachmentGid={}, reason=missing_download_url",
                            flowId, taskGid, attachmentGid);
                    continue;
                }

                log.info("attachment detail resolved - flowId={}, attachmentGid={}, fileName={}, contentType={}, size={}",
                        flowId, attachmentGid, fileName, contentType, attachment.getSize());

                UploadLinkResponse uploadLink = serviceAssetsClient.getUploadLink(
                        fileName, contentType, flowId, attachmentGid);
                log.info("upload link resolved - flowId={}, attachmentGid={}, objectKey={}",
                        flowId, attachmentGid, uploadLink.getObjectKey());

                fileTransferService.transferFile(
                        attachment.getDownloadUrl(),
                        uploadLink.getUploadLink(),
                        contentType,
                        attachment.getSize(),
                        flowId,
                        attachmentGid);
                log.info("binary upload completed - flowId={}, attachmentGid={}, objectKey={}",
                        flowId, attachmentGid, uploadLink.getObjectKey());

                AssetResponse asset = serviceAssetsClient.finalizeUpload(
                        uploadLink.getObjectKey(), flowId, attachmentGid);
                results.add(asset);

                log.info("attachment finalized - flowId={}, attachmentGid={}, assetId={}",
                        flowId, attachmentGid, asset.getId());
            } catch (Exception e) {
                log.error("attachment processing failed - flowId={}, task={}, attachmentGid={}, error={}",
                        flowId, taskGid, attachmentGid, e.getMessage(), e);
            }
        }

        log.info("upload batch finished - flowId={}, task={}, successCount={}, attachmentCount={}",
                flowId, taskGid, results.size(), attachmentGids.size());
        return CompletableFuture.completedFuture(results);
    }
}
