package com.flowboard.workspace.repository;

import com.flowboard.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WorkspaceRepository
        extends JpaRepository<Workspace, Integer> {

    List<Workspace> findByOwnerId(Integer ownerId);
    List<Workspace> findByVisibility(Workspace.Visibility visibility);
    boolean existsByNameAndOwnerId(String name, Integer ownerId);
    long countByOwnerId(Integer ownerId);
}