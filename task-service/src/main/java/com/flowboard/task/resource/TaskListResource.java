package com.flowboard.task.resource;

import com.flowboard.task.config.JwtConfig;
import com.flowboard.task.dto.*;
import com.flowboard.task.service.TaskListService;
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
@RequestMapping("/api/v1/lists")
@RequiredArgsConstructor
@Tag(name = "List / Column Management",
        description = "Manage Kanban board lists/columns")
@SecurityRequirement(name = "bearerAuth")
public class TaskListResource {

    private final TaskListService listService;
    private final JwtConfig jwtConfig;

    @PostMapping
    @Operation(summary = "Create a new list on a board")
    public ResponseEntity<TaskListResponse> create(
            @Valid @RequestBody TaskListRequest request,
            @RequestHeader("Authorization") String bearer) {
        return ResponseEntity.status(201).body(
                listService.createList(request, getUserId(bearer)));
    }

    @GetMapping("/{listId}")
    @Operation(summary = "Get list by ID")
    public ResponseEntity<TaskListResponse> getById(
            @PathVariable Integer listId) {
        return ResponseEntity.ok(listService.getListById(listId));
    }

    @GetMapping("/board/{boardId}")
    @Operation(summary = "Get all active lists for a board "
            + "(ordered by position)")
    public ResponseEntity<List<TaskListResponse>> getByBoard(
            @PathVariable Integer boardId) {
        return ResponseEntity.ok(
                listService.getListsByBoard(boardId));
    }

    @GetMapping("/board/{boardId}/archived")
    @Operation(summary = "Get all archived lists for a board")
    public ResponseEntity<List<TaskListResponse>> getArchivedLists(
            @PathVariable Integer boardId) {
        return ResponseEntity.ok(
                listService.getArchivedLists(boardId));
    }

    @PutMapping("/{listId}")
    @Operation(summary = "Update list name or color")
    public ResponseEntity<TaskListResponse> update(
            @PathVariable Integer listId,
            @RequestBody TaskListRequest request) {
        return ResponseEntity.ok(
                listService.updateList(listId, request));
    }

    @PutMapping("/board/{boardId}/reorder")
    @Operation(summary = "Reorder lists on a board "
            + "— drag and drop position update")
    public ResponseEntity<Map<String, String>> reorder(
            @PathVariable Integer boardId,
            @Valid @RequestBody ReorderRequest request) {
        listService.reorderLists(boardId, request);
        return ResponseEntity.ok(
                Map.of("message", "Lists reordered successfully"));
    }

    @PutMapping("/{listId}/archive")
    @Operation(summary = "Archive a list (soft delete)")
    public ResponseEntity<TaskListResponse> archive(
            @PathVariable Integer listId) {
        return ResponseEntity.ok(listService.archiveList(listId));
    }

    @PutMapping("/{listId}/unarchive")
    @Operation(summary = "Unarchive a list (restore)")
    public ResponseEntity<TaskListResponse> unarchive(
            @PathVariable Integer listId) {
        return ResponseEntity.ok(listService.unarchiveList(listId));
    }

    @PutMapping("/{listId}/move")
    @Operation(summary = "Move a list to another board "
            + "in the same workspace")
    public ResponseEntity<TaskListResponse> move(
            @PathVariable Integer listId,
            @Valid @RequestBody MoveListRequest request) {
        return ResponseEntity.ok(
                listService.moveList(listId, request));
    }

    @DeleteMapping("/{listId}")
    @Operation(summary = "Permanently delete an archived list")
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable Integer listId) {
        listService.deleteList(listId);
        return ResponseEntity.ok(
                Map.of("message", "List deleted successfully"));
    }

    private Integer getUserId(String bearer) {
        return jwtConfig.getUserIdFromToken(bearer.substring(7));
    }
}