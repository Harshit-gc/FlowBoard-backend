package com.flowboard.workspace.resource;

import com.flowboard.workspace.config.JwtConfig;
import com.flowboard.workspace.dto.*;
import com.flowboard.workspace.entity.WorkspaceMember;
import com.flowboard.workspace.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
@Tag(name = "Workspace Management",
        description = "Create and manage workspaces and their members")
@SecurityRequirement(name = "bearerAuth")
public class WorkspaceResource {

    private final WorkspaceService workspaceService;
    private final JwtConfig jwtConfig;

    // ── Workspace CRUD ────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create a new workspace")
    public ResponseEntity<WorkspaceResponse> create(
            @Valid @RequestBody WorkspaceRequest request,
            @RequestHeader("Authorization") String bearer) {
        return ResponseEntity.status(201).body(
                workspaceService.createWorkspace(
                        request, getUserId(bearer)));
    }

    @GetMapping("/{workspaceId}")
    @Operation(summary = "Get workspace by ID")
    public ResponseEntity<WorkspaceResponse> getById(
            @PathVariable Integer workspaceId) {
        return ResponseEntity.ok(
                workspaceService.getById(workspaceId));
    }

    @GetMapping("/my")
    @Operation(summary = "Get all workspaces for current user "
            + "(owned + member of)")
    public ResponseEntity<List<WorkspaceResponse>> getMyWorkspaces(
            @RequestHeader("Authorization") String bearer) {
        return ResponseEntity.ok(
                workspaceService.getMyWorkspaces(getUserId(bearer)));
    }

    @GetMapping("/owner/{ownerId}")
    @Operation(summary = "Get workspaces owned by a user")
    public ResponseEntity<List<WorkspaceResponse>> getByOwner(
            @PathVariable Integer ownerId) {
        return ResponseEntity.ok(
                workspaceService.getByOwner(ownerId));
    }

    @GetMapping("/member/{userId}")
    @Operation(summary = "Get workspaces where user is a member")
    public ResponseEntity<List<WorkspaceResponse>> getByMember(
            @PathVariable Integer userId) {
        return ResponseEntity.ok(
                workspaceService.getByMember(userId));
    }

    @GetMapping("/public")
    @Operation(summary = "Get all public workspaces (no auth required)")
    public ResponseEntity<List<WorkspaceResponse>> getPublic() {
        return ResponseEntity.ok(
                workspaceService.getPublicWorkspaces());
    }

    @GetMapping("/all")
    @Operation(summary = "Get all workspaces — Platform Admin only")
    public ResponseEntity<List<WorkspaceResponse>> getAllWorkspaces(
            @RequestHeader("Authorization") String bearer) {
        String role = jwtConfig.getRoleFromToken(bearer.substring(7));
        if (!"PLATFORM_ADMIN".equals(role)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(workspaceService.getAllWorkspaces());
    }

    @PutMapping("/{workspaceId}")
    @Operation(summary = "Update workspace details")
    public ResponseEntity<WorkspaceResponse> update(
            @PathVariable Integer workspaceId,
            @Valid @RequestBody WorkspaceRequest request,
            @RequestHeader("Authorization") String bearer) {
        String token = bearer.substring(7);
        return ResponseEntity.ok(workspaceService.updateWorkspace(
                workspaceId, request,
                getUserId(bearer),
                jwtConfig.getRoleFromToken(token)));
    }

    @DeleteMapping("/{workspaceId}")
    @Operation(summary = "Delete a workspace permanently")
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable Integer workspaceId,
            @RequestHeader("Authorization") String bearer) {
        String token = bearer.substring(7);
        workspaceService.deleteWorkspace(
                workspaceId,
                getUserId(bearer),
                jwtConfig.getRoleFromToken(token));
        return ResponseEntity.ok(
                Map.of("message", "Workspace deleted successfully"));
    }

    // ── Member Management ─────────────────────────────────────────────────────

    @GetMapping("/{workspaceId}/members")
    @Operation(summary = "Get all members of a workspace")
    public ResponseEntity<List<WorkspaceMember>> getMembers(
            @PathVariable Integer workspaceId) {
        return ResponseEntity.ok(
                workspaceService.getMembers(workspaceId));
    }

    @PostMapping("/{workspaceId}/members")
    @Operation(summary = "Add a member to workspace")
    public ResponseEntity<WorkspaceMember> addMember(
            @PathVariable Integer workspaceId,
            @Valid @RequestBody AddMemberRequest request,
            @RequestHeader("Authorization") String bearer) {
        return ResponseEntity.status(201).body(
                workspaceService.addMember(
                        workspaceId, request, getUserId(bearer)));
    }

    @DeleteMapping("/{workspaceId}/members/{userId}")
    @Operation(summary = "Remove a member from workspace")
    public ResponseEntity<Map<String, String>> removeMember(
            @PathVariable Integer workspaceId,
            @PathVariable Integer userId,
            @RequestHeader("Authorization") String bearer) {
        workspaceService.removeMember(
                workspaceId, userId, getUserId(bearer));
        return ResponseEntity.ok(
                Map.of("message", "Member removed successfully"));
    }

    @PutMapping("/{workspaceId}/members/{userId}/role")
    @Operation(summary = "Update a workspace member's role")
    public ResponseEntity<WorkspaceMember> updateRole(
            @PathVariable Integer workspaceId,
            @PathVariable Integer userId,
            @Valid @RequestBody UpdateMemberRoleRequest request,
            @RequestHeader("Authorization") String bearer) {
        return ResponseEntity.ok(workspaceService.updateMemberRole(
                workspaceId, userId, request, getUserId(bearer)));
    }

    @GetMapping("/{workspaceId}/members/{userId}/check")
    @Operation(summary = "Check if a user is a workspace member")
    public ResponseEntity<Map<String, Boolean>> isMember(
            @PathVariable Integer workspaceId,
            @PathVariable Integer userId) {
        return ResponseEntity.ok(Map.of("isMember",
                workspaceService.isMember(workspaceId, userId)));
    }

    private Integer getUserId(String bearer) {
        return jwtConfig.getUserIdFromToken(bearer.substring(7));
    }
}