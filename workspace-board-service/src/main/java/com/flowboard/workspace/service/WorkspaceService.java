package com.flowboard.workspace.service;

import com.flowboard.workspace.dto.*;
import com.flowboard.workspace.entity.WorkspaceMember;
import java.util.List;

public interface WorkspaceService {

    WorkspaceResponse createWorkspace(WorkspaceRequest request,
                                      Integer ownerId);
    WorkspaceResponse getById(Integer workspaceId);
    List<WorkspaceResponse> getByOwner(Integer ownerId);
    List<WorkspaceResponse> getByMember(Integer userId);
    List<WorkspaceResponse> getMyWorkspaces(Integer userId);
    List<WorkspaceResponse> getPublicWorkspaces();
    WorkspaceResponse updateWorkspace(Integer workspaceId,
                                      WorkspaceRequest request,
                                      Integer requesterId);
    void deleteWorkspace(Integer workspaceId, Integer requesterId);

    // Member operations
    WorkspaceMember addMember(Integer workspaceId,
                              AddMemberRequest request,
                              Integer requesterId);
    void removeMember(Integer workspaceId,
                      Integer userId,
                      Integer requesterId);
    WorkspaceMember updateMemberRole(Integer workspaceId,
                                     Integer userId,
                                     UpdateMemberRoleRequest request,
                                     Integer requesterId);
    List<WorkspaceMember> getMembers(Integer workspaceId);
    boolean isMember(Integer workspaceId, Integer userId);
}