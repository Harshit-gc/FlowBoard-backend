package com.flowboard.task.service;

import com.flowboard.task.dto.*;
import java.util.List;

public interface CardService {

    // CRUD
    CardResponse createCard(CardRequest request, Integer createdById);
    CardResponse getCardById(Integer cardId);
    List<CardResponse> getCardsByList(Integer listId);
    List<CardResponse> getCardsByBoard(Integer boardId);
    List<CardResponse> getCardsByAssignee(Integer assigneeId);
    CardResponse updateCard(Integer cardId,
                            CardRequest request,
                            Integer actorId);

    // Move and reorder
    CardResponse moveCard(Integer cardId,
                          MoveCardRequest request,
                          Integer actorId);
    void reorderCards(Integer listId,
                      ReorderRequest request);

    // Archival
    CardResponse archiveCard(Integer cardId, Integer actorId);
    CardResponse unarchiveCard(Integer cardId, Integer actorId);
    List<CardResponse> getArchivedCards(Integer boardId);

    // Delete
    void deleteCard(Integer cardId);

    // Assignment and priority
    CardResponse setAssignee(Integer cardId,
                             AssigneeRequest request,
                             Integer actorId);
    CardResponse setPriority(Integer cardId,
                             PriorityRequest request,
                             Integer actorId);
    CardResponse setStatus(Integer cardId,
                           StatusRequest request,
                           Integer actorId);

    // Overdue
    List<CardResponse> getOverdueCards();
    List<CardResponse> getOverdueCardsByBoard(Integer boardId);

    // Search
    List<CardResponse> searchCards(Integer boardId, String keyword);

    // Activity log
    List<CardActivityResponse> getCardActivity(Integer cardId);
}