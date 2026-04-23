package com.flowboard.workspace.service;

import com.flowboard.workspace.dto.*;
import com.flowboard.workspace.entity.BoardMember;
import java.util.List;

public interface BoardService {

    BoardResponse createBoard(BoardRequest request, Integer createdById);
    BoardResponse getBoardById(Integer boardId);
    List<BoardResponse> getBoardsByWorkspace(Integer workspaceId);
    List<BoardResponse> getBoardsByMember(Integer userId);
    List<BoardResponse> getClosedBoards(Integer workspaceId);
    List<BoardResponse> getAllBoards();
    BoardResponse updateBoard(Integer boardId,
                              BoardRequest request,
                              Integer requesterId);
    BoardResponse closeBoard(Integer boardId, Integer requesterId);
    BoardResponse reopenBoard(Integer boardId, Integer requesterId);
    void deleteBoard(Integer boardId, Integer requesterId);

    // Member operations
    BoardMember addMember(Integer boardId,
                          AddMemberRequest request,
                          Integer requesterId);
    void removeMember(Integer boardId,
                      Integer userId,
                      Integer requesterId);
    BoardMember updateMemberRole(Integer boardId,
                                 Integer userId,
                                 UpdateMemberRoleRequest request,
                                 Integer requesterId);
    List<BoardMember> getMembers(Integer boardId);

    // Analytics
    BoardAnalyticsResponse getBoardAnalytics(Integer boardId);
}