package com.flowboard.workspace.service.impl;

import com.flowboard.workspace.dto.*;
import com.flowboard.workspace.entity.Workspace;
import com.flowboard.workspace.entity.WorkspaceMember;
import com.flowboard.workspace.exception.AppException;
import com.flowboard.workspace.repository.WorkspaceMemberRepository;
import com.flowboard.workspace.repository.WorkspaceRepository;
import com.flowboard.workspace.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;

    @Override
    public WorkspaceResponse createWorkspace(WorkspaceRequest request,
                                             Integer ownerId) {
        // Check duplicate name for same owner
        if (workspaceRepository.existsByNameAndOwnerId(
                request.getName(), ownerId)) {
            throw new AppException(
                    "Workspace with this name already exists",
                    HttpStatus.CONFLICT);
        }

        Workspace workspace = Workspace.builder()
                .name(request.getName())
                .description(request.getDescription())
                .ownerId(ownerId)
                .logoUrl(request.getLogoUrl())
                .visibility(Workspace.Visibility
                        .valueOf(request.getVisibility().toUpperCase()))
                .build();
        workspace = workspaceRepository.save(workspace);

        // Auto-add owner as ADMIN member
        WorkspaceMember ownerMember = WorkspaceMember.builder()
                .workspaceId(workspace.getWorkspaceId())
                .userId(ownerId)
                .role(WorkspaceMember.Role.ADMIN)
                .build();
        memberRepository.save(ownerMember);

        return toResponse(workspace);
    }

    @Override
    public WorkspaceResponse getById(Integer workspaceId) {
        return toResponse(findWorkspace(workspaceId));
    }

    @Override
    public List<WorkspaceResponse> getByOwner(Integer ownerId) {
        return workspaceRepository.findByOwnerId(ownerId)
                .stream().map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<WorkspaceResponse> getByMember(Integer userId) {
        List<Integer> ids = memberRepository
                .findWorkspaceIdsByUserId(userId);
        return workspaceRepository.findAllById(ids)
                .stream().map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<WorkspaceResponse> getMyWorkspaces(Integer userId) {
        // Returns workspaces owned by user + workspaces user is member of
        List<WorkspaceResponse> owned = getByOwner(userId);
        List<WorkspaceResponse> memberOf = getByMember(userId);

        // Merge and deduplicate by workspaceId
        List<WorkspaceResponse> all = new ArrayList<>(owned);
        memberOf.stream()
                .filter(m -> owned.stream().noneMatch(
                        o -> o.getWorkspaceId()
                                .equals(m.getWorkspaceId())))
                .forEach(all::add);
        return all;
    }

    @Override
    public List<WorkspaceResponse> getPublicWorkspaces() {
        return workspaceRepository
                .findByVisibility(Workspace.Visibility.PUBLIC)
                .stream().map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<WorkspaceResponse> getAllWorkspaces() {
        return workspaceRepository.findAll()
                .stream().map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public WorkspaceResponse updateWorkspace(Integer workspaceId,
                                             WorkspaceRequest request,
                                             Integer requesterId,
                                             String requesterRole) {
        Workspace workspace = findWorkspace(workspaceId);

        if (!"PLATFORM_ADMIN".equals(requesterRole)) {
            validateOwnerOrAdmin(workspace, requesterId);
        }

        if (request.getName() != null)
            workspace.setName(request.getName());
        if (request.getDescription() != null)
            workspace.setDescription(request.getDescription());
        if (request.getLogoUrl() != null)
            workspace.setLogoUrl(request.getLogoUrl());
        if (request.getVisibility() != null)
            workspace.setVisibility(Workspace.Visibility
                    .valueOf(request.getVisibility().toUpperCase()));

        return toResponse(workspaceRepository.save(workspace));
    }

    @Override
    @Transactional
    public void deleteWorkspace(Integer workspaceId,
                                Integer requesterId,
                                String requesterRole) {
        Workspace workspace = findWorkspace(workspaceId);
        if (!"PLATFORM_ADMIN".equals(requesterRole)) {
            validateOwnerOrAdmin(workspace, requesterId);
        }

        memberRepository.findByWorkspaceId(workspaceId)
                .forEach(memberRepository::delete);
        workspaceRepository.delete(workspace);
    }

    @Override
    public WorkspaceMember addMember(Integer workspaceId,
                                     AddMemberRequest request,
                                     Integer requesterId) {
        Workspace workspace = findWorkspace(workspaceId);
        validateOwnerOrAdmin(workspace, requesterId);

        if (memberRepository.existsByWorkspaceIdAndUserId(
                workspaceId, request.getUserId())) {
            throw new AppException(
                    "User is already a member",
                    HttpStatus.CONFLICT);
        }
        WorkspaceMember member = WorkspaceMember.builder()
                .workspaceId(workspaceId)
                .userId(request.getUserId())
                .role(WorkspaceMember.Role
                        .valueOf(request.getRole().toUpperCase()))
                .build();
        return memberRepository.save(member);
    }

    @Override
    @Transactional
    public void removeMember(Integer workspaceId,
                             Integer userId,
                             Integer requesterId) {
        Workspace workspace = findWorkspace(workspaceId);
        validateOwnerOrAdmin(workspace, requesterId);

        // Owner cannot be removed
        if (workspace.getOwnerId().equals(userId)) {
            throw new AppException(
                    "Cannot remove workspace owner",
                    HttpStatus.BAD_REQUEST);
        }
        memberRepository.deleteByWorkspaceIdAndUserId(workspaceId, userId);
    }

    @Override
    public WorkspaceMember updateMemberRole(Integer workspaceId,
                                            Integer userId,
                                            UpdateMemberRoleRequest request,
                                            Integer requesterId) {
        Workspace workspace = findWorkspace(workspaceId);
        validateOwnerOrAdmin(workspace, requesterId);

        WorkspaceMember member = memberRepository
                .findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new AppException(
                        "Member not found", HttpStatus.NOT_FOUND));
        member.setRole(WorkspaceMember.Role
                .valueOf(request.getRole().toUpperCase()));
        return memberRepository.save(member);
    }

    @Override
    public List<WorkspaceMember> getMembers(Integer workspaceId) {
        findWorkspace(workspaceId);
        return memberRepository.findByWorkspaceId(workspaceId);
    }

    @Override
    public boolean isMember(Integer workspaceId, Integer userId) {
        return memberRepository.existsByWorkspaceIdAndUserId(
                workspaceId, userId);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Workspace findWorkspace(Integer id) {
        return workspaceRepository.findById(id)
                .orElseThrow(() -> new AppException(
                        "Workspace not found", HttpStatus.NOT_FOUND));
    }

    private void validateOwnerOrAdmin(Workspace workspace,
                                      Integer requesterId) {
        if (!workspace.getOwnerId().equals(requesterId)) {
            boolean isAdmin = memberRepository
                    .findByWorkspaceIdAndUserId(
                            workspace.getWorkspaceId(), requesterId)
                    .map(m -> m.getRole() == WorkspaceMember.Role.ADMIN)
                    .orElse(false);
            if (!isAdmin) {
                throw new AppException(
                        "Access denied — must be owner or admin",
                        HttpStatus.FORBIDDEN);
            }
        }
    }

    private WorkspaceResponse toResponse(Workspace w) {
        int memberCount = memberRepository
                .findByWorkspaceId(w.getWorkspaceId()).size();
        return WorkspaceResponse.builder()
                .workspaceId(w.getWorkspaceId())
                .name(w.getName())
                .description(w.getDescription())
                .ownerId(w.getOwnerId())
                .visibility(w.getVisibility().name())
                .logoUrl(w.getLogoUrl())
                .memberCount(memberCount)
                .createdAt(w.getCreatedAt())
                .updatedAt(w.getUpdatedAt())
                .build();
    }
}