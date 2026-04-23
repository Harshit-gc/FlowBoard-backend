package com.flowboard.task.service.impl;

import com.flowboard.task.dto.*;
import com.flowboard.task.entity.TaskList;
import com.flowboard.task.exception.AppException;
import com.flowboard.task.repository.CardRepository;
import com.flowboard.task.repository.TaskListRepository;
import com.flowboard.task.service.TaskListService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskListServiceImpl implements TaskListService {

    private final TaskListRepository listRepository;
    private final CardRepository cardRepository;

    @Override
    public TaskListResponse createList(TaskListRequest request,
                                       Integer createdById) {
        // Get max position for this board and add 1
        int position = listRepository
                .findMaxPosition(request.getBoardId())
                .map(p -> p + 1)
                .orElse(1);

        TaskList list = TaskList.builder()
                .boardId(request.getBoardId())
                .name(request.getName())
                .position(position)
                .color(request.getColor())
                .isArchived(false)
                .build();

        return toResponse(listRepository.save(list));
    }

    @Override
    public TaskListResponse getListById(Integer listId) {
        return toResponse(findList(listId));
    }

    @Override
    public List<TaskListResponse> getListsByBoard(Integer boardId) {
        return listRepository
                .findByBoardIdAndIsArchivedOrderByPosition(
                        boardId, false)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public TaskListResponse updateList(Integer listId,
                                       TaskListRequest request) {
        TaskList list = findList(listId);
        if (request.getName() != null)
            list.setName(request.getName());
        if (request.getColor() != null)
            list.setColor(request.getColor());
        return toResponse(listRepository.save(list));
    }

    @Override
    @Transactional
    public void reorderLists(Integer boardId,
                             ReorderRequest request) {
        List<Integer> orderedIds = request.getOrderedIds();
        // Atomically update all positions
        for (int i = 0; i < orderedIds.size(); i++) {
            TaskList list = findList(orderedIds.get(i));
            if (!list.getBoardId().equals(boardId)) {
                throw new AppException(
                        "List does not belong to this board",
                        HttpStatus.BAD_REQUEST);
            }
            list.setPosition(i + 1);
            listRepository.save(list);
        }
    }

    @Override
    public TaskListResponse archiveList(Integer listId) {
        TaskList list = findList(listId);
        if (list.isArchived()) {
            throw new AppException(
                    "List is already archived",
                    HttpStatus.BAD_REQUEST);
        }
        list.setArchived(true);
        return toResponse(listRepository.save(list));
    }

    @Override
    public TaskListResponse unarchiveList(Integer listId) {
        TaskList list = findList(listId);
        if (!list.isArchived()) {
            throw new AppException(
                    "List is not archived",
                    HttpStatus.BAD_REQUEST);
        }
        list.setArchived(false);
        return toResponse(listRepository.save(list));
    }

    @Override
    public List<TaskListResponse> getArchivedLists(Integer boardId) {
        return listRepository
                .findByBoardIdAndIsArchivedOrderByPosition(
                        boardId, true)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteList(Integer listId) {
        TaskList list = findList(listId);
        if (!list.isArchived()) {
            throw new AppException(
                    "Archive the list before deleting",
                    HttpStatus.BAD_REQUEST);
        }
        listRepository.delete(list);
    }

    @Override
    public TaskListResponse moveList(Integer listId,
                                     MoveListRequest request) {
        TaskList list = findList(listId);

        // Get max position in target board
        int newPosition = listRepository
                .findMaxPosition(request.getTargetBoardId())
                .map(p -> p + 1)
                .orElse(1);

        list.setBoardId(request.getTargetBoardId());
        list.setPosition(newPosition);
        return toResponse(listRepository.save(list));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TaskList findList(Integer listId) {
        return listRepository.findById(listId)
                .orElseThrow(() -> new AppException(
                        "List not found", HttpStatus.NOT_FOUND));
    }

    private TaskListResponse toResponse(TaskList l) {
        long cardCount = cardRepository.countByListId(l.getListId());
        return TaskListResponse.builder()
                .listId(l.getListId())
                .boardId(l.getBoardId())
                .name(l.getName())
                .position(l.getPosition())
                .color(l.getColor())
                .isArchived(l.isArchived())
                .cardCount(cardCount)
                .createdAt(l.getCreatedAt())
                .updatedAt(l.getUpdatedAt())
                .build();
    }
}