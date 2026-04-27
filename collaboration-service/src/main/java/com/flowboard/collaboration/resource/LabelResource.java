package com.flowboard.collaboration.resource;

import com.flowboard.collaboration.dto.*;
import com.flowboard.collaboration.service.LabelService;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Label & Checklist Management",
        description = "Board labels, card-label associations, " +
                "checklists and checklist items")
@SecurityRequirement(name = "bearerAuth")
public class LabelResource {

    private final LabelService labelService;

    // ── Label Endpoints ───────────────────────────────────────────────────────

    @PostMapping("/labels")
    @Operation(summary = "Create a new label on a board")
    public ResponseEntity<LabelResponse> createLabel(
            @Valid @RequestBody LabelRequest request) {
        return ResponseEntity.status(201).body(
                labelService.createLabel(request));
    }

    @GetMapping("/labels/{labelId}")
    @Operation(summary = "Get label by ID")
    public ResponseEntity<LabelResponse> getLabelById(
            @PathVariable Integer labelId) {
        return ResponseEntity.ok(
                labelService.getLabelById(labelId));
    }

    @GetMapping("/boards/{boardId}/labels")
    @Operation(summary = "Get all labels for a board")
    public ResponseEntity<List<LabelResponse>> getLabelsByBoard(
            @PathVariable Integer boardId) {
        return ResponseEntity.ok(
                labelService.getLabelsByBoard(boardId));
    }

    @PutMapping("/labels/{labelId}")
    @Operation(summary = "Update a label name or color")
    public ResponseEntity<LabelResponse> updateLabel(
            @PathVariable Integer labelId,
            @RequestBody LabelRequest request) {
        return ResponseEntity.ok(
                labelService.updateLabel(labelId, request));
    }

    @DeleteMapping("/labels/{labelId}")
    @Operation(summary = "Delete a label from a board")
    public ResponseEntity<Map<String, String>> deleteLabel(
            @PathVariable Integer labelId) {
        labelService.deleteLabel(labelId);
        return ResponseEntity.ok(
                Map.of("message", "Label deleted"));
    }

    // ── Card-Label Association Endpoints ──────────────────────────────────────

    @PostMapping("/cards/{cardId}/labels/{labelId}")
    @Operation(summary = "Add a label to a card")
    public ResponseEntity<Map<String, String>> addLabelToCard(
            @PathVariable Integer cardId,
            @PathVariable Integer labelId) {
        labelService.addLabelToCard(cardId, labelId);
        return ResponseEntity.status(201).body(
                Map.of("message", "Label added to card"));
    }

    @DeleteMapping("/cards/{cardId}/labels/{labelId}")
    @Operation(summary = "Remove a label from a card")
    public ResponseEntity<Map<String, String>> removeLabelFromCard(
            @PathVariable Integer cardId,
            @PathVariable Integer labelId) {
        labelService.removeLabelFromCard(cardId, labelId);
        return ResponseEntity.ok(
                Map.of("message", "Label removed from card"));
    }

    @GetMapping("/cards/{cardId}/labels")
    @Operation(summary = "Get all labels on a card")
    public ResponseEntity<List<LabelResponse>> getLabelsForCard(
            @PathVariable Integer cardId) {
        return ResponseEntity.ok(
                labelService.getLabelsForCard(cardId));
    }

    // ── Checklist Endpoints ───────────────────────────────────────────────────

    @PostMapping("/checklists")
    @Operation(summary = "Create a checklist on a card")
    public ResponseEntity<ChecklistResponse> createChecklist(
            @Valid @RequestBody ChecklistRequest request) {
        return ResponseEntity.status(201).body(
                labelService.createChecklist(request));
    }

    @GetMapping("/checklists/{checklistId}")
    @Operation(summary = "Get checklist by ID with all items")
    public ResponseEntity<ChecklistResponse> getChecklistById(
            @PathVariable Integer checklistId) {
        return ResponseEntity.ok(
                labelService.getChecklistById(checklistId));
    }

    @GetMapping("/cards/{cardId}/checklists")
    @Operation(summary = "Get all checklists for a card")
    public ResponseEntity<List<ChecklistResponse>> getByCard(
            @PathVariable Integer cardId) {
        return ResponseEntity.ok(
                labelService.getChecklistsByCard(cardId));
    }

    @DeleteMapping("/checklists/{checklistId}")
    @Operation(summary = "Delete a checklist and all its items")
    public ResponseEntity<Map<String, String>> deleteChecklist(
            @PathVariable Integer checklistId) {
        labelService.deleteChecklist(checklistId);
        return ResponseEntity.ok(
                Map.of("message", "Checklist deleted"));
    }

    @GetMapping("/checklists/{checklistId}/progress")
    @Operation(summary = "Get checklist completion progress")
    public ResponseEntity<ChecklistProgressResponse> getProgress(
            @PathVariable Integer checklistId) {
        return ResponseEntity.ok(
                labelService.getChecklistProgress(checklistId));
    }

    // ── Checklist Item Endpoints ──────────────────────────────────────────────

    @PostMapping("/checklist-items")
    @Operation(summary = "Add an item to a checklist")
    public ResponseEntity<ChecklistItemResponse> addItem(
            @Valid @RequestBody ChecklistItemRequest request) {
        return ResponseEntity.status(201).body(
                labelService.addItem(request));
    }

    @GetMapping("/checklists/{checklistId}/items")
    @Operation(summary = "Get all items in a checklist")
    public ResponseEntity<List<ChecklistItemResponse>> getItems(
            @PathVariable Integer checklistId) {
        return ResponseEntity.ok(
                labelService.getItemsByChecklist(checklistId));
    }

    @PutMapping("/checklist-items/{itemId}/toggle")
    @Operation(summary = "Toggle checklist item complete/incomplete")
    public ResponseEntity<ChecklistItemResponse> toggleItem(
            @PathVariable Integer itemId) {
        return ResponseEntity.ok(labelService.toggleItem(itemId));
    }

    @DeleteMapping("/checklist-items/{itemId}")
    @Operation(summary = "Delete a checklist item")
    public ResponseEntity<Map<String, String>> deleteItem(
            @PathVariable Integer itemId) {
        labelService.deleteItem(itemId);
        return ResponseEntity.ok(
                Map.of("message", "Item deleted"));
    }
}