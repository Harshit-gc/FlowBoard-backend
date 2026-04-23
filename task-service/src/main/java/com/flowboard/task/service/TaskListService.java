package com.flowboard.task.service;

import com.flowboard.task.dto.*;
import java.util.List;

public interface TaskListService {

    // CRUD
    TaskListResponse createList(TaskListRequest request,
                                Integer createdById);
    TaskListResponse getListById(Integer listId);
    List<TaskListResponse> getListsByBoard(Integer boardId);
    TaskListResponse updateList(Integer listId,
                                TaskListRequest request);

    // Position management
    void reorderLists(Integer boardId, ReorderRequest request);

    // Archival
    TaskListResponse archiveList(Integer listId);
    TaskListResponse unarchiveList(Integer listId);
    List<TaskListResponse> getArchivedLists(Integer boardId);

    // Delete
    void deleteList(Integer listId);

    // Move to another board
    TaskListResponse moveList(Integer listId,
                              MoveListRequest request);
}