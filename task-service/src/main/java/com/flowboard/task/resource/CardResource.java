package com.flowboard.task.resource;

import com.flowboard.task.config.JwtConfig;
import com.flowboard.task.dto.*;
import com.flowboard.task.service.CardService;
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
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
@Tag(name = "Card / Task Management",
        description = "Manage task cards with full metadata")
@SecurityRequirement(name = "bearerAuth")
public class CardResource {

    private final CardService cardService;
    private final JwtConfig jwtConfig;

    // ── Card CRUD ─────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create a new card in a list")
    public ResponseEntity<CardResponse> create(
            @Valid @RequestBody CardRequest request,
            @RequestHeader("Authorization") String bearer) {
        return ResponseEntity.status(201).body(
                cardService.createCard(request, getUserId(bearer)));
    }

    @GetMapping("/{cardId}")
    @Operation(summary = "Get card by ID")
    public ResponseEntity<CardResponse> getById(
            @PathVariable Integer cardId) {
        return ResponseEntity.ok(cardService.getCardById(cardId));
    }

    @GetMapping("/list/{listId}")
    @Operation(summary = "Get all active cards in a list "
            + "(ordered by position)")
    public ResponseEntity<List<CardResponse>> getByList(
            @PathVariable Integer listId) {
        return ResponseEntity.ok(cardService.getCardsByList(listId));
    }

    @GetMapping("/board/{boardId}")
    @Operation(summary = "Get all active cards in a board")
    public ResponseEntity<List<CardResponse>> getByBoard(
            @PathVariable Integer boardId) {
        return ResponseEntity.ok(cardService.getCardsByBoard(boardId));
    }

    @GetMapping("/assignee/{assigneeId}")
    @Operation(summary = "Get all cards assigned to a user")
    public ResponseEntity<List<CardResponse>> getByAssignee(
            @PathVariable Integer assigneeId) {
        return ResponseEntity.ok(
                cardService.getCardsByAssignee(assigneeId));
    }

    @GetMapping("/board/{boardId}/archived")
    @Operation(summary = "Get all archived cards in a board")
    public ResponseEntity<List<CardResponse>> getArchived(
            @PathVariable Integer boardId) {
        return ResponseEntity.ok(
                cardService.getArchivedCards(boardId));
    }

    @PutMapping("/{cardId}")
    @Operation(summary = "Update card details")
    public ResponseEntity<CardResponse> update(
            @PathVariable Integer cardId,
            @RequestBody CardRequest request,
            @RequestHeader("Authorization") String bearer) {
        return ResponseEntity.ok(cardService.updateCard(
                cardId, request, getUserId(bearer)));
    }

    // ── Move and Reorder ──────────────────────────────────────────────────────

    @PutMapping("/{cardId}/move")
    @Operation(summary = "Move card to a different list or board")
    public ResponseEntity<CardResponse> move(
            @PathVariable Integer cardId,
            @Valid @RequestBody MoveCardRequest request,
            @RequestHeader("Authorization") String bearer) {
        return ResponseEntity.ok(cardService.moveCard(
                cardId, request, getUserId(bearer)));
    }

    @PutMapping("/list/{listId}/reorder")
    @Operation(summary = "Reorder cards within a list "
            + "— drag and drop position update")
    public ResponseEntity<Map<String, String>> reorder(
            @PathVariable Integer listId,
            @Valid @RequestBody ReorderRequest request) {
        cardService.reorderCards(listId, request);
        return ResponseEntity.ok(
                Map.of("message", "Cards reordered successfully"));
    }

    // ── Archive ───────────────────────────────────────────────────────────────

    @PutMapping("/{cardId}/archive")
    @Operation(summary = "Archive a card (soft delete)")
    public ResponseEntity<CardResponse> archive(
            @PathVariable Integer cardId,
            @RequestHeader("Authorization") String bearer) {
        return ResponseEntity.ok(
                cardService.archiveCard(cardId, getUserId(bearer)));
    }

    @PutMapping("/{cardId}/unarchive")
    @Operation(summary = "Unarchive a card (restore)")
    public ResponseEntity<CardResponse> unarchive(
            @PathVariable Integer cardId,
            @RequestHeader("Authorization") String bearer) {
        return ResponseEntity.ok(
                cardService.unarchiveCard(cardId, getUserId(bearer)));
    }

    @DeleteMapping("/{cardId}")
    @Operation(summary = "Permanently delete an archived card")
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable Integer cardId) {
        cardService.deleteCard(cardId);
        return ResponseEntity.ok(
                Map.of("message", "Card deleted successfully"));
    }

    // ── Assignment, Priority, Status ──────────────────────────────────────────

    @PutMapping("/{cardId}/assignee")
    @Operation(summary = "Assign or unassign a member to a card")
    public ResponseEntity<CardResponse> setAssignee(
            @PathVariable Integer cardId,
            @RequestBody AssigneeRequest request,
            @RequestHeader("Authorization") String bearer) {
        return ResponseEntity.ok(cardService.setAssignee(
                cardId, request, getUserId(bearer)));
    }

    @PutMapping("/{cardId}/priority")
    @Operation(summary = "Set card priority "
            + "(LOW / MEDIUM / HIGH / CRITICAL)")
    public ResponseEntity<CardResponse> setPriority(
            @PathVariable Integer cardId,
            @Valid @RequestBody PriorityRequest request,
            @RequestHeader("Authorization") String bearer) {
        return ResponseEntity.ok(cardService.setPriority(
                cardId, request, getUserId(bearer)));
    }

    @PutMapping("/{cardId}/status")
    @Operation(summary = "Set card status "
            + "(TO_DO / IN_PROGRESS / IN_REVIEW / DONE)")
    public ResponseEntity<CardResponse> setStatus(
            @PathVariable Integer cardId,
            @Valid @RequestBody StatusRequest request,
            @RequestHeader("Authorization") String bearer) {
        return ResponseEntity.ok(cardService.setStatus(
                cardId, request, getUserId(bearer)));
    }

    // ── Overdue and Search ────────────────────────────────────────────────────

    @GetMapping("/overdue")
    @Operation(summary = "Get all overdue cards across the platform")
    public ResponseEntity<List<CardResponse>> getOverdue() {
        return ResponseEntity.ok(cardService.getOverdueCards());
    }

    @GetMapping("/board/{boardId}/overdue")
    @Operation(summary = "Get overdue cards for a specific board")
    public ResponseEntity<List<CardResponse>> getOverdueByBoard(
            @PathVariable Integer boardId) {
        return ResponseEntity.ok(
                cardService.getOverdueCardsByBoard(boardId));
    }

    @GetMapping("/board/{boardId}/search")
    @Operation(summary = "Search cards by title within a board")
    public ResponseEntity<List<CardResponse>> search(
            @PathVariable Integer boardId,
            @RequestParam String keyword) {
        return ResponseEntity.ok(
                cardService.searchCards(boardId, keyword));
    }

    // ── Activity Log ──────────────────────────────────────────────────────────

    @GetMapping("/{cardId}/activity")
    @Operation(summary = "Get full activity log for a card")
    public ResponseEntity<List<CardActivityResponse>> getActivity(
            @PathVariable Integer cardId) {
        return ResponseEntity.ok(
                cardService.getCardActivity(cardId));
    }

    private Integer getUserId(String bearer) {
        return jwtConfig.getUserIdFromToken(bearer.substring(7));
    }
}