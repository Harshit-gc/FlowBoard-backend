package com.flowboard.collaboration.service.impl;

import com.flowboard.collaboration.dto.*;
import com.flowboard.collaboration.entity.*;
import com.flowboard.collaboration.exception.AppException;
import com.flowboard.collaboration.repository.*;
import com.flowboard.collaboration.service.LabelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LabelServiceImpl implements LabelService {

    private final LabelRepository labelRepository;
    private final CardLabelRepository cardLabelRepository;
    private final ChecklistRepository checklistRepository;
    private final ChecklistItemRepository checklistItemRepository;

    // ── Label CRUD ────────────────────────────────────────────────────────────

    @Override
    public LabelResponse createLabel(LabelRequest request) {
        if (labelRepository.existsByNameAndBoardId(
                request.getName(), request.getBoardId())) {
            throw new AppException(
                    "Label with this name already exists on board",
                    HttpStatus.CONFLICT);
        }
        Label label = Label.builder()
                .boardId(request.getBoardId())
                .name(request.getName())
                .color(request.getColor())
                .build();
        return toLabelResponse(labelRepository.save(label));
    }

    @Override
    public LabelResponse getLabelById(Integer labelId) {
        return toLabelResponse(findLabel(labelId));
    }

    @Override
    public List<LabelResponse> getLabelsByBoard(Integer boardId) {
        return labelRepository.findByBoardId(boardId)
                .stream()
                .map(this::toLabelResponse)
                .collect(Collectors.toList());
    }

    @Override
    public LabelResponse updateLabel(Integer labelId,
                                     LabelRequest request) {
        Label label = findLabel(labelId);
        if (request.getName() != null)
            label.setName(request.getName());
        if (request.getColor() != null)
            label.setColor(request.getColor());
        return toLabelResponse(labelRepository.save(label));
    }

    @Override
    @Transactional
    public void deleteLabel(Integer labelId) {
        findLabel(labelId);
        // Remove all card associations first
        cardLabelRepository.findByLabelId(labelId)
                .forEach(cardLabelRepository::delete);
        labelRepository.deleteById(labelId);
    }

    // ── Card-Label Association ────────────────────────────────────────────────

    @Override
    public void addLabelToCard(Integer cardId, Integer labelId) {
        findLabel(labelId);
        if (cardLabelRepository.existsByCardIdAndLabelId(
                cardId, labelId)) {
            throw new AppException(
                    "Label already added to this card",
                    HttpStatus.CONFLICT);
        }
        cardLabelRepository.save(CardLabel.builder()
                .cardId(cardId)
                .labelId(labelId)
                .build());
    }

    @Override
    @Transactional
    public void removeLabelFromCard(Integer cardId,
                                    Integer labelId) {
        if (!cardLabelRepository.existsByCardIdAndLabelId(
                cardId, labelId)) {
            throw new AppException(
                    "Label not found on this card",
                    HttpStatus.NOT_FOUND);
        }
        cardLabelRepository.deleteByCardIdAndLabelId(
                cardId, labelId);
    }

    @Override
    public List<LabelResponse> getLabelsForCard(Integer cardId) {
        return cardLabelRepository.findByCardId(cardId)
                .stream()
                .map(cl -> toLabelResponse(
                        findLabel(cl.getLabelId())))
                .collect(Collectors.toList());
    }

    // ── Checklist Operations ──────────────────────────────────────────────────

    @Override
    public ChecklistResponse createChecklist(
            ChecklistRequest request) {
        int position = checklistRepository
                .findMaxPosition(request.getCardId())
                .map(p -> p + 1)
                .orElse(1);

        Checklist checklist = Checklist.builder()
                .cardId(request.getCardId())
                .title(request.getTitle())
                .position(position)
                .build();

        return toChecklistResponse(
                checklistRepository.save(checklist));
    }

    @Override
    public ChecklistResponse getChecklistById(
            Integer checklistId) {
        return toChecklistResponse(findChecklist(checklistId));
    }

    @Override
    public List<ChecklistResponse> getChecklistsByCard(
            Integer cardId) {
        return checklistRepository
                .findByCardIdOrderByPosition(cardId)
                .stream()
                .map(this::toChecklistResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteChecklist(Integer checklistId) {
        findChecklist(checklistId);
        // Delete all items first
        checklistItemRepository.deleteByChecklistId(checklistId);
        checklistRepository.deleteById(checklistId);
    }

    // ── Checklist Item Operations ─────────────────────────────────────────────

    @Override
    public ChecklistItemResponse addItem(
            ChecklistItemRequest request) {
        findChecklist(request.getChecklistId());
        ChecklistItem item = ChecklistItem.builder()
                .checklistId(request.getChecklistId())
                .text(request.getText())
                .isCompleted(false)
                .assigneeId(request.getAssigneeId())
                .dueDate(request.getDueDate())
                .build();
        return toItemResponse(checklistItemRepository.save(item));
    }

    @Override
    public ChecklistItemResponse toggleItem(Integer itemId) {
        ChecklistItem item = findItem(itemId);
        item.setCompleted(!item.isCompleted());
        return toItemResponse(checklistItemRepository.save(item));
    }

    @Override
    public void deleteItem(Integer itemId) {
        findItem(itemId);
        checklistItemRepository.deleteById(itemId);
    }

    @Override
    public List<ChecklistItemResponse> getItemsByChecklist(
            Integer checklistId) {
        findChecklist(checklistId);
        return checklistItemRepository
                .findByChecklistId(checklistId)
                .stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ChecklistProgressResponse getChecklistProgress(
            Integer checklistId) {
        Checklist checklist = findChecklist(checklistId);
        long total = checklistItemRepository
                .countByChecklistId(checklistId);
        long completed = checklistItemRepository
                .countByChecklistIdAndIsCompleted(
                        checklistId, true);
        int percent = total == 0
                ? 0
                : (int) ((completed * 100) / total);

        return ChecklistProgressResponse.builder()
                .checklistId(checklistId)
                .title(checklist.getTitle())
                .totalItems((int) total)
                .completedItems((int) completed)
                .progressPercent(percent)
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Label findLabel(Integer labelId) {
        return labelRepository.findById(labelId)
                .orElseThrow(() -> new AppException(
                        "Label not found", HttpStatus.NOT_FOUND));
    }

    private Checklist findChecklist(Integer checklistId) {
        return checklistRepository.findById(checklistId)
                .orElseThrow(() -> new AppException(
                        "Checklist not found",
                        HttpStatus.NOT_FOUND));
    }

    private ChecklistItem findItem(Integer itemId) {
        return checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(
                        "Checklist item not found",
                        HttpStatus.NOT_FOUND));
    }

    private LabelResponse toLabelResponse(Label l) {
        return LabelResponse.builder()
                .labelId(l.getLabelId())
                .boardId(l.getBoardId())
                .name(l.getName())
                .color(l.getColor())
                .createdAt(l.getCreatedAt())
                .build();
    }

    private ChecklistResponse toChecklistResponse(Checklist c) {
        List<ChecklistItemResponse> items =
                checklistItemRepository
                        .findByChecklistId(c.getChecklistId())
                        .stream()
                        .map(this::toItemResponse)
                        .collect(Collectors.toList());

        long total = items.size();
        long completed = items.stream()
                .filter(ChecklistItemResponse::isCompleted)
                .count();
        int percent = total == 0
                ? 0
                : (int) ((completed * 100) / total);

        return ChecklistResponse.builder()
                .checklistId(c.getChecklistId())
                .cardId(c.getCardId())
                .title(c.getTitle())
                .position(c.getPosition())
                .totalItems((int) total)
                .completedItems((int) completed)
                .progressPercent(percent)
                .items(items)
                .createdAt(c.getCreatedAt())
                .build();
    }

    private ChecklistItemResponse toItemResponse(ChecklistItem i) {
        return ChecklistItemResponse.builder()
                .itemId(i.getItemId())
                .checklistId(i.getChecklistId())
                .text(i.getText())
                .isCompleted(i.isCompleted())
                .assigneeId(i.getAssigneeId())
                .dueDate(i.getDueDate())
                .build();
    }
}