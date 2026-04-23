package com.flowboard.task.service.impl;

import com.flowboard.task.dto.*;
import com.flowboard.task.entity.Card;
import com.flowboard.task.entity.CardActivity;
import com.flowboard.task.exception.AppException;
import com.flowboard.task.repository.CardActivityRepository;
import com.flowboard.task.repository.CardRepository;
import com.flowboard.task.service.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepository;
    private final CardActivityRepository activityRepository;

    @Override
    public CardResponse createCard(CardRequest request,
                                   Integer createdById) {
        int position = cardRepository
                .findMaxPositionByListId(request.getListId())
                .map(p -> p + 1)
                .orElse(1);

        Card card = Card.builder()
                .listId(request.getListId())
                .boardId(request.getBoardId())
                .title(request.getTitle())
                .description(request.getDescription())
                .position(position)
                .priority(request.getPriority() != null
                        ? Card.Priority.valueOf(request.getPriority())
                        : Card.Priority.MEDIUM)
                .status(request.getStatus() != null
                        ? Card.Status.valueOf(request.getStatus())
                        : Card.Status.TO_DO)
                .dueDate(request.getDueDate())
                .startDate(request.getStartDate())
                .assigneeId(request.getAssigneeId())
                .createdById(createdById)
                .coverColor(request.getCoverColor())
                .isArchived(false)
                .build();

        card = cardRepository.save(card);
        logActivity(card.getCardId(), createdById,
                "CREATED", null, card.getTitle());
        return toResponse(card);
    }

    @Override
    public CardResponse getCardById(Integer cardId) {
        return toResponse(findCard(cardId));
    }

    @Override
    public List<CardResponse> getCardsByList(Integer listId) {
        return cardRepository
                .findByListIdOrderByPosition(listId)
                .stream()
                .filter(c -> !c.isArchived())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CardResponse> getCardsByBoard(Integer boardId) {
        return cardRepository
                .findByBoardIdOrderByPosition(boardId)
                .stream()
                .filter(c -> !c.isArchived())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CardResponse> getCardsByAssignee(Integer assigneeId) {
        return cardRepository.findByAssigneeId(assigneeId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CardResponse updateCard(Integer cardId,
                                   CardRequest request,
                                   Integer actorId) {
        Card card = findCard(cardId);

        if (request.getTitle() != null) {
            logActivity(cardId, actorId, "TITLE_CHANGED",
                    card.getTitle(), request.getTitle());
            card.setTitle(request.getTitle());
        }
        if (request.getDescription() != null)
            card.setDescription(request.getDescription());
        if (request.getPriority() != null) {
            logActivity(cardId, actorId, "PRIORITY_CHANGED",
                    card.getPriority().name(), request.getPriority());
            card.setPriority(Card.Priority
                    .valueOf(request.getPriority()));
        }
        if (request.getStatus() != null) {
            logActivity(cardId, actorId, "STATUS_CHANGED",
                    card.getStatus().name(), request.getStatus());
            card.setStatus(Card.Status.valueOf(request.getStatus()));
        }
        if (request.getDueDate() != null) {
            logActivity(cardId, actorId, "DUE_DATE_CHANGED",
                    String.valueOf(card.getDueDate()),
                    String.valueOf(request.getDueDate()));
            card.setDueDate(request.getDueDate());
        }
        if (request.getStartDate() != null)
            card.setStartDate(request.getStartDate());
        if (request.getCoverColor() != null)
            card.setCoverColor(request.getCoverColor());

        return toResponse(cardRepository.save(card));
    }

    @Override
    @Transactional
    public CardResponse moveCard(Integer cardId,
                                 MoveCardRequest request,
                                 Integer actorId) {
        Card card = findCard(cardId);
        String oldList = String.valueOf(card.getListId());

        // Get position in target list
        int newPosition = request.getPosition() != null
                ? request.getPosition()
                : cardRepository
                  .findMaxPositionByListId(request.getTargetListId())
                  .map(p -> p + 1)
                  .orElse(1);

        card.setListId(request.getTargetListId());
        card.setBoardId(request.getTargetBoardId());
        card.setPosition(newPosition);

        logActivity(cardId, actorId, "MOVED",
                "list:" + oldList,
                "list:" + request.getTargetListId());

        return toResponse(cardRepository.save(card));
    }

    @Override
    @Transactional
    public void reorderCards(Integer listId,
                             ReorderRequest request) {
        List<Integer> orderedIds = request.getOrderedIds();
        for (int i = 0; i < orderedIds.size(); i++) {
            Card card = findCard(orderedIds.get(i));
            if (!card.getListId().equals(listId)) {
                throw new AppException(
                        "Card does not belong to this list",
                        HttpStatus.BAD_REQUEST);
            }
            card.setPosition(i + 1);
            cardRepository.save(card);
        }
    }

    @Override
    public CardResponse archiveCard(Integer cardId, Integer actorId) {
        Card card = findCard(cardId);
        if (card.isArchived()) {
            throw new AppException(
                    "Card is already archived",
                    HttpStatus.BAD_REQUEST);
        }
        card.setArchived(true);
        logActivity(cardId, actorId, "ARCHIVED", "false", "true");
        return toResponse(cardRepository.save(card));
    }

    @Override
    public CardResponse unarchiveCard(Integer cardId,
                                      Integer actorId) {
        Card card = findCard(cardId);
        if (!card.isArchived()) {
            throw new AppException(
                    "Card is not archived",
                    HttpStatus.BAD_REQUEST);
        }
        card.setArchived(false);
        logActivity(cardId, actorId, "UNARCHIVED", "true", "false");
        return toResponse(cardRepository.save(card));
    }

    @Override
    public List<CardResponse> getArchivedCards(Integer boardId) {
        return cardRepository
                .findByBoardIdAndIsArchived(boardId, true)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteCard(Integer cardId) {
        Card card = findCard(cardId);
        if (!card.isArchived()) {
            throw new AppException(
                    "Archive the card before deleting",
                    HttpStatus.BAD_REQUEST);
        }
        activityRepository
                .findByCardIdOrderByCreatedAtDesc(cardId)
                .forEach(activityRepository::delete);
        cardRepository.delete(card);
    }

    @Override
    public CardResponse setAssignee(Integer cardId,
                                    AssigneeRequest request,
                                    Integer actorId) {
        Card card = findCard(cardId);
        String oldAssignee = String.valueOf(card.getAssigneeId());
        card.setAssigneeId(request.getAssigneeId());
        logActivity(cardId, actorId, "ASSIGNEE_CHANGED",
                oldAssignee,
                String.valueOf(request.getAssigneeId()));
        return toResponse(cardRepository.save(card));
    }

    @Override
    public CardResponse setPriority(Integer cardId,
                                    PriorityRequest request,
                                    Integer actorId) {
        Card card = findCard(cardId);
        String oldPriority = card.getPriority().name();
        card.setPriority(Card.Priority.valueOf(request.getPriority()));
        logActivity(cardId, actorId, "PRIORITY_CHANGED",
                oldPriority, request.getPriority());
        return toResponse(cardRepository.save(card));
    }

    @Override
    public CardResponse setStatus(Integer cardId,
                                  StatusRequest request,
                                  Integer actorId) {
        Card card = findCard(cardId);
        String oldStatus = card.getStatus().name();
        card.setStatus(Card.Status.valueOf(request.getStatus()));
        logActivity(cardId, actorId, "STATUS_CHANGED",
                oldStatus, request.getStatus());
        return toResponse(cardRepository.save(card));
    }

    @Override
    public List<CardResponse> getOverdueCards() {
        return cardRepository.findOverdueCards(LocalDate.now())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CardResponse> getOverdueCardsByBoard(Integer boardId) {
        return cardRepository
                .findOverdueCardsByBoard(boardId, LocalDate.now())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CardResponse> searchCards(Integer boardId,
                                          String keyword) {
        return cardRepository.searchByTitle(boardId, keyword)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CardActivityResponse> getCardActivity(Integer cardId) {
        findCard(cardId);
        return activityRepository
                .findByCardIdOrderByCreatedAtDesc(cardId)
                .stream()
                .map(a -> CardActivityResponse.builder()
                        .activityId(a.getActivityId())
                        .cardId(a.getCardId())
                        .actorId(a.getActorId())
                        .action(a.getAction())
                        .oldValue(a.getOldValue())
                        .newValue(a.getNewValue())
                        .createdAt(a.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Card findCard(Integer cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new AppException(
                        "Card not found", HttpStatus.NOT_FOUND));
    }

    private void logActivity(Integer cardId, Integer actorId,
                             String action,
                             String oldValue, String newValue) {
        activityRepository.save(CardActivity.builder()
                .cardId(cardId)
                .actorId(actorId)
                .action(action)
                .oldValue(oldValue)
                .newValue(newValue)
                .build());
    }

    private boolean isOverdue(Card card) {
        return card.getDueDate() != null
                && LocalDate.now().isAfter(card.getDueDate())
                && card.getStatus() != Card.Status.DONE;
    }

    private CardResponse toResponse(Card c) {
        return CardResponse.builder()
                .cardId(c.getCardId())
                .listId(c.getListId())
                .boardId(c.getBoardId())
                .title(c.getTitle())
                .description(c.getDescription())
                .position(c.getPosition())
                .priority(c.getPriority().name())
                .status(c.getStatus().name())
                .dueDate(c.getDueDate())
                .startDate(c.getStartDate())
                .assigneeId(c.getAssigneeId())
                .createdById(c.getCreatedById())
                .isArchived(c.isArchived())
                .isOverdue(isOverdue(c))
                .coverColor(c.getCoverColor())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}