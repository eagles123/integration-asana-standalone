package com.baker.integration.asana.service;

import com.baker.integration.asana.config.LinkFlowProperties;
import com.baker.integration.asana.model.connection.AsanaLinkState;
import com.baker.integration.asana.model.connection.LinkStatePayload;
import com.baker.integration.asana.repository.AsanaLinkStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AsanaLinkStateService {

    private final LinkFlowProperties linkFlowProperties;
    private final AsanaLinkStateRepository asanaLinkStateRepository;
    private final StateTokenService stateTokenService;

    public AsanaLinkStateService(LinkFlowProperties linkFlowProperties,
                                 AsanaLinkStateRepository asanaLinkStateRepository,
                                 StateTokenService stateTokenService) {
        this.linkFlowProperties = linkFlowProperties;
        this.asanaLinkStateRepository = asanaLinkStateRepository;
        this.stateTokenService = stateTokenService;
    }

    @Transactional
    public String createSignedState(String asanaWorkspaceGid, String asanaUserGid, String tenantId) {
        asanaLinkStateRepository.deleteByExpiresAtBefore(Instant.now());

        String nonce = UUID.randomUUID().toString().replace("-", "");
        Instant expiresAt = Instant.now().plusSeconds(linkFlowProperties.getStateTtlSeconds());

        AsanaLinkState linkState = new AsanaLinkState();
        linkState.setStateNonce(nonce);
        linkState.setAsanaWorkspaceGid(asanaWorkspaceGid);
        linkState.setAsanaUserGid(asanaUserGid);
        linkState.setTenantId(tenantId);
        linkState.setExpiresAt(expiresAt);
        asanaLinkStateRepository.save(linkState);

        LinkStatePayload payload = new LinkStatePayload();
        payload.setNonce(nonce);
        payload.setAsanaWorkspaceGid(asanaWorkspaceGid);
        payload.setAsanaUserGid(asanaUserGid);
        payload.setTenantId(tenantId);
        payload.setExpEpochSeconds(expiresAt.getEpochSecond());
        return stateTokenService.sign(payload);
    }

    @Transactional
    public LinkStatePayload verifyAndConsume(String signedState) {
        LinkStatePayload payload = verifyWithoutConsume(signedState);
        AsanaLinkState persisted = asanaLinkStateRepository.findByStateNonce(payload.getNonce())
                .orElseThrow(() -> new IllegalArgumentException("State nonce not found or already consumed"));
        asanaLinkStateRepository.delete(persisted);
        return payload;
    }

    public LinkStatePayload verifyWithoutConsume(String signedState) {
        LinkStatePayload payload = stateTokenService.verifyAndParse(signedState);
        AsanaLinkState persisted = asanaLinkStateRepository.findByStateNonce(payload.getNonce())
                .orElseThrow(() -> new IllegalArgumentException("State nonce not found or already consumed"));

        if (persisted.getExpiresAt().isBefore(Instant.now())) {
            asanaLinkStateRepository.delete(persisted);
            throw new IllegalArgumentException("State nonce has expired");
        }

        if (!persisted.getAsanaWorkspaceGid().equals(payload.getAsanaWorkspaceGid())
                || !persisted.getAsanaUserGid().equals(payload.getAsanaUserGid())
                || !persisted.getTenantId().equals(payload.getTenantId())) {
            throw new IllegalArgumentException("State payload mismatch");
        }

        return payload;
    }
}
