package com.baker.integration.asana.repository;

import com.baker.integration.asana.model.connection.AsanaLinkState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface AsanaLinkStateRepository extends JpaRepository<AsanaLinkState, Long> {
    Optional<AsanaLinkState> findByStateNonce(String stateNonce);
    void deleteByExpiresAtBefore(Instant cutoff);
}
