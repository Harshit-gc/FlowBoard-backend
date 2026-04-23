package com.flowboard.workspace.resource;

import com.flowboard.workspace.config.JwtConfig;
import com.flowboard.workspace.dto.*;
import com.flowboard.workspace.entity.BoardMember;
import com.flowboard.workspace.service.BoardService;
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
@RequestMapping("/api/v1/boards")
@RequiredArgsConstructor
@Tag(name = "Board Management",
        description = "Create and manage Kanban boards and their members")
@SecurityRequirement(name = "bearerAuth")
public class BoardResource {

    private final BoardService boardService;
    private final JwtConfig jwtConfig;

    // ── Board CRUD ────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create a new board in a workspace")
    public ResponseEntity<BoardResponse> create(
            @Valid @RequestBody BoardRequest request,
            @RequestHeader("Authorization") String bearer) {
        return ResponseEntity.status(201).body(
                boardService.createBoard(request, getUserId(bearer)));
    }

    @GetMapping("/{boardId}")
    @Operation(summary = "Get board by ID")
    public ResponseEntity<BoardResponse> getById(
            @PathVariable Integer boardId) {
        return ResponseEntity.ok(boardService.getBoardById(boardId));
    }

    @GetMapping("/workspace/{workspaceId}")
    @Operation(summary = "Get all active boards in a workspace")
    public ResponseEntity<List<BoardResponse>> getByWorkspace(
            @PathVariable Integer workspaceId) {
        return ResponseEntity.ok(
                boardService.getBoardsByWorkspace(workspaceId));
    }

    @GetMapping("/workspace/{workspaceId}/closed")
    @Operation(summary = "Get all closed boards in a workspace")
    public ResponseEntity<List<BoardResponse>> getClosedBoards(
            @PathVariable Integer workspaceId) {
        return ResponseEntity.ok(
                boardService.getClosedBoards(workspaceId));
    }

    @GetMapping("/member/{userId}")
    @Operation(summary = "Get boards where user is a member")
    public ResponseEntity<List<BoardResponse>> getByMember(
            @PathVariable Integer userId) {
        return ResponseEntity.ok(
                boardService.getBoardsByMember(userId));
    }

    @GetMapping
    @Operation(summary = "Get all boards — Platform Admin only")
    public ResponseEntity<List<BoardResponse>> getAll() {
        return ResponseEntity.ok(boardService.getAllBoards());
    }

    @PutMapping("/{boardId}")
    @Operation(summary = "Update board details")
    public ResponseEntity<BoardResponse> update(
            @PathVariable Integer boardId,
            @Valid @RequestBody BoardRequest request,
            @RequestHeader("Authorization") String bearer) {
        return ResponseEntity.ok(boardService.updateBoard(
                boardId, request, getUserId(bearer)));
    }

    @PutMapping("/{boardId}/close")
    @Operation(summary = "Close a board — prevents further changes")
    public ResponseEntity<BoardResponse> close(
            @PathVariable Integer boardId,
            @RequestHeader("Authorization") String bearer) {
        return ResponseEntity.ok(
                boardService.closeBoard(boardId, getUserId(bearer)));
    }

    @PutMapping("/{boardId}/reopen")
    @Operation(summary = "Reopen a closed board")
    public ResponseEntity<BoardResponse> reopen(
            @PathVariable Integer boardId,
            @RequestHeader("Authorization") String bearer) {
        return ResponseEntity.ok(
                boardService.reopenBoard(boardId, getUserId(bearer)));
    }

    @DeleteMapping("/{boardId}")
    @Operation(summary = "Delete a board permanently")
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable Integer boardId,
            @RequestHeader("Authorization") String bearer) {
        boardService.deleteBoard(boardId, getUserId(bearer));
        return ResponseEntity.ok(
                Map.of("message", "Board deleted successfully"));
    }

    // ── Analytics ─────────────────────────────────────────────────────────────

    @GetMapping("/{boardId}/analytics")
    @Operation(summary = "Get board analytics — member count, "
            + "card counts, completion rate")
    public ResponseEntity<BoardAnalyticsResponse> getAnalytics(
            @PathVariable Integer boardId) {
        return ResponseEntity.ok(
                boardService.getBoardAnalytics(boardId));
    }

    // ── Member Management ─────────────────────────────────────────────────────

    @GetMapping("/{boardId}/members")
    @Operation(summary = "Get all members of a board")
    public ResponseEntity<List<BoardMember>> getMembers(
            @PathVariable Integer boardId) {
        return ResponseEntity.ok(boardService.getMembers(boardId));
    }

    @PostMapping("/{boardId}/members")
    @Operation(summary = "Add a member to a board")
    public ResponseEntity<BoardMember> addMember(
            @PathVariable Integer boardId,
            @Valid @RequestBody AddMemberRequest request,
            @RequestHeader("Authorization") String bearer) {
        return ResponseEntity.status(201).body(
                boardService.addMember(
                        boardId, request, getUserId(bearer)));
    }

    @DeleteMapping("/{boardId}/members/{userId}")
    @Operation(summary = "Remove a member from a board")
    public ResponseEntity<Map<String, String>> removeMember(
            @PathVariable Integer boardId,
            @PathVariable Integer userId,
            @RequestHeader("Authorization") String bearer) {
        boardService.removeMember(boardId, userId, getUserId(bearer));
        return ResponseEntity.ok(
                Map.of("message", "Member removed successfully"));
    }

    @PutMapping("/{boardId}/members/{userId}/role")
    @Operation(summary = "Update a board member's role")
    public ResponseEntity<BoardMember> updateRole(
            @PathVariable Integer boardId,
            @PathVariable Integer userId,
            @Valid @RequestBody UpdateMemberRoleRequest request,
            @RequestHeader("Authorization") String bearer) {
        return ResponseEntity.ok(boardService.updateMemberRole(
                boardId, userId, request, getUserId(bearer)));
    }

    private Integer getUserId(String bearer) {
        return jwtConfig.getUserIdFromToken(bearer.substring(7));
    }
}