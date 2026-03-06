package com.baker.integration.asana.repository;

import com.baker.integration.asana.model.connection.AsanaConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AsanaConnectionRepository extends JpaRepository<AsanaConnection, Long> {
    Optional<AsanaConnection> findByAsanaWorkspaceGidAndAsanaUserGid(String asanaWorkspaceGid, String asanaUserGid);
}
