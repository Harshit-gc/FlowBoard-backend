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
    List<BoardResponse> getPublicBoards();

    BoardResponse updateBoard(Integer boardId, BoardRequest request,
                              Integer requesterId, boolean isPlatformAdmin);
    BoardResponse closeBoard(Integer boardId, Integer requesterId,
                             boolean isPlatformAdmin);
    BoardResponse reopenBoard(Integer boardId, Integer requesterId,
                              boolean isPlatformAdmin);
    void deleteBoard(Integer boardId, Integer requesterId,
                     boolean isPlatformAdmin);

    BoardMember addMember(Integer boardId, AddMemberRequest request,
                          Integer requesterId, boolean isPlatformAdmin);
    void removeMember(Integer boardId, Integer userId,
                      Integer requesterId, boolean isPlatformAdmin);
    BoardMember updateMemberRole(Integer boardId, Integer userId,
                                 UpdateMemberRoleRequest request,
                                 Integer requesterId, boolean isPlatformAdmin);
    List<BoardMember> getMembers(Integer boardId);
    BoardAnalyticsResponse getBoardAnalytics(Integer boardId);
}