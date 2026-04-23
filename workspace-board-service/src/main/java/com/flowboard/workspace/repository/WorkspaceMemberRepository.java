package com.flowboard.workspace.repository;

import com.flowboard.workspace.entity.WorkspaceMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceMemberRepository
        extends JpaRepository<WorkspaceMember, Integer> {

    List<WorkspaceMember> findByWorkspaceId(Integer workspaceId);
    List<WorkspaceMember> findByUserId(Integer userId);
    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(
            Integer workspaceId, Integer userId);
    boolean existsByWorkspaceIdAndUserId(
            Integer workspaceId, Integer userId);
    void deleteByWorkspaceIdAndUserId(
            Integer workspaceId, Integer userId);

    @Query("SELECT wm.workspaceId FROM WorkspaceMember wm WHERE wm.userId = :userId")
    List<Integer> findWorkspaceIdsByUserId(Integer userId);
}