package com.flowboard.workspace.service.impl;

import com.flowboard.workspace.client.TaskServiceClient;
import com.flowboard.workspace.dto.*;
import com.flowboard.workspace.entity.Board;
import com.flowboard.workspace.entity.BoardMember;
import com.flowboard.workspace.entity.Workspace;
import com.flowboard.workspace.exception.AppException;
import com.flowboard.workspace.repository.BoardMemberRepository;
import com.flowboard.workspace.repository.BoardRepository;
import com.flowboard.workspace.repository.WorkspaceRepository;
import com.flowboard.workspace.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {

    private final BoardRepository boardRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final TaskServiceClient taskServiceClient;
    private final WorkspaceRepository workspaceRepository;

    @Override
    public BoardResponse createBoard(BoardRequest request,
                                     Integer createdById) {
        Board board = Board.builder()
                .workspaceId(request.getWorkspaceId())
                .name(request.getName())
                .description(request.getDescription())
                .background(request.getBackground())
                .visibility(Board.Visibility
                        .valueOf(request.getVisibility().toUpperCase()))
                .createdById(createdById)
                .isClosed(false)
                .build();
        board = boardRepository.save(board);

        BoardMember creatorMember = BoardMember.builder()
                .boardId(board.getBoardId())
                .userId(createdById)
                .role(BoardMember.Role.ADMIN)
                .build();
        boardMemberRepository.save(creatorMember);

        return toResponse(board);
    }

    @Override
    public BoardResponse getBoardById(Integer boardId) {
        return toResponse(findBoard(boardId));
    }

    @Override
    public List<BoardResponse> getBoardsByWorkspace(Integer workspaceId) {
        return boardRepository
                .findByWorkspaceIdAndIsClosed(workspaceId, false)
                .stream().map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<BoardResponse> getBoardsByMember(Integer userId) {
        List<Integer> boardIds = boardMemberRepository
                .findBoardIdsByUserId(userId);
        return boardRepository.findAllById(boardIds)
                .stream().map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<BoardResponse> getClosedBoards(Integer workspaceId) {
        return boardRepository
                .findByWorkspaceIdAndIsClosed(workspaceId, true)
                .stream().map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<BoardResponse> getAllBoards() {
        return boardRepository.findAll()
                .stream().map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<BoardResponse> getPublicBoards() {
        return boardRepository.findByVisibility(Board.Visibility.PUBLIC)
                .stream()
                .filter(b -> !b.isClosed())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BoardResponse updateBoard(Integer boardId,
                                     BoardRequest request,
                                     Integer requesterId,
                                     boolean isPlatformAdmin) {
        Board board = findBoard(boardId);
        validateBoardAdmin(board, requesterId, isPlatformAdmin);

        if (request.getName() != null)
            board.setName(request.getName());
        if (request.getDescription() != null)
            board.setDescription(request.getDescription());
        if (request.getBackground() != null)
            board.setBackground(request.getBackground());
        if (request.getVisibility() != null)
            board.setVisibility(Board.Visibility
                    .valueOf(request.getVisibility().toUpperCase()));

        return toResponse(boardRepository.save(board));
    }

    @Override
    public BoardResponse closeBoard(Integer boardId,
                                    Integer requesterId,
                                    boolean isPlatformAdmin) {
        Board board = findBoard(boardId);
        validateBoardAdmin(board, requesterId, isPlatformAdmin);
        if (board.isClosed()) {
            throw new AppException(
                    "Board is already closed",
                    HttpStatus.BAD_REQUEST);
        }
        board.setClosed(true);
        return toResponse(boardRepository.save(board));
    }

    @Override
    public BoardResponse reopenBoard(Integer boardId,
                                     Integer requesterId,
                                     boolean isPlatformAdmin) {
        Board board = findBoard(boardId);
        validateBoardAdmin(board, requesterId, isPlatformAdmin);
        if (!board.isClosed()) {
            throw new AppException(
                    "Board is not closed",
                    HttpStatus.BAD_REQUEST);
        }
        board.setClosed(false);
        return toResponse(boardRepository.save(board));
    }

    @Override
    @Transactional
    public void deleteBoard(Integer boardId,
                            Integer requesterId,
                            boolean isPlatformAdmin) {
        Board board = findBoard(boardId);
        validateBoardAdmin(board, requesterId, isPlatformAdmin);
        boardMemberRepository.findByBoardId(boardId)
                .forEach(boardMemberRepository::delete);
        boardRepository.delete(board);
    }

    @Override
    public BoardMember addMember(Integer boardId,
                                 AddMemberRequest request,
                                 Integer requesterId,
                                 boolean isPlatformAdmin) {
        Board board = findBoard(boardId);
        validateBoardAdmin(board, requesterId, isPlatformAdmin);

        if (boardMemberRepository.existsByBoardIdAndUserId(
                boardId, request.getUserId())) {
            throw new AppException(
                    "User is already a board member",
                    HttpStatus.CONFLICT);
        }
        BoardMember member = BoardMember.builder()
                .boardId(boardId)
                .userId(request.getUserId())
                .role(BoardMember.Role
                        .valueOf(request.getRole().toUpperCase()))
                .build();
        return boardMemberRepository.save(member);
    }

    @Override
    @Transactional
    public void removeMember(Integer boardId,
                             Integer userId,
                             Integer requesterId,
                             boolean isPlatformAdmin) {
        Board board = findBoard(boardId);
        validateBoardAdmin(board, requesterId, isPlatformAdmin);

        if (board.getCreatedById().equals(userId)) {
            throw new AppException(
                    "Cannot remove board creator",
                    HttpStatus.BAD_REQUEST);
        }
        boardMemberRepository.deleteByBoardIdAndUserId(boardId, userId);
    }

    @Override
    public BoardMember updateMemberRole(Integer boardId,
                                        Integer userId,
                                        UpdateMemberRoleRequest request,
                                        Integer requesterId,
                                        boolean isPlatformAdmin) {
        Board board = findBoard(boardId);
        validateBoardAdmin(board, requesterId, isPlatformAdmin);

        BoardMember member = boardMemberRepository
                .findByBoardIdAndUserId(boardId, userId)
                .orElseThrow(() -> new AppException(
                        "Board member not found", HttpStatus.NOT_FOUND));
        member.setRole(BoardMember.Role
                .valueOf(request.getRole().toUpperCase()));
        return boardMemberRepository.save(member);
    }

    @Override
    public List<BoardMember> getMembers(Integer boardId) {
        findBoard(boardId);
        return boardMemberRepository.findByBoardId(boardId);
    }

    @Override
    public BoardAnalyticsResponse getBoardAnalytics(Integer boardId) {
        Board board = findBoard(boardId);
        int memberCount = boardMemberRepository
                .findByBoardId(boardId).size();
        long totalCards = taskServiceClient.getCardCountByBoard(boardId);
        long totalLists = taskServiceClient.getListCountByBoard(boardId);

        return BoardAnalyticsResponse.builder()
                .boardId(board.getBoardId())
                .boardName(board.getName())
                .totalMembers(memberCount)
                .totalCards((int) totalCards)
                .totalLists((int) totalLists)
                .isClosed(board.isClosed())
                .build();
    }


    // ── Helpers ──────────────────────────────────────────────────────────────

    private Board findBoard(Integer boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new AppException(
                        "Board not found", HttpStatus.NOT_FOUND));
    }

    private void validateBoardAdmin(Board board,
                                    Integer requesterId,
                                    boolean isPlatformAdmin) {
        if (isPlatformAdmin) return;

        boolean isAdmin = boardMemberRepository
                .findByBoardIdAndUserId(
                        board.getBoardId(), requesterId)
                .map(m -> m.getRole() == BoardMember.Role.ADMIN)
                .orElse(false);
        if (!isAdmin && !board.getCreatedById().equals(requesterId)) {
            throw new AppException(
                    "Access denied — must be board admin",
                    HttpStatus.FORBIDDEN);
        }
    }

    private BoardResponse toResponse(Board b) {
        int memberCount = boardMemberRepository
                .findByBoardId(b.getBoardId()).size();

        String workspaceName = workspaceRepository
                .findById(b.getWorkspaceId())
                .map(Workspace::getName)
                .orElse(null);

        return BoardResponse.builder()
                .boardId(b.getBoardId())
                .workspaceId(b.getWorkspaceId())
                .workspaceName(workspaceName)
                .name(b.getName())
                .description(b.getDescription())
                .background(b.getBackground())
                .visibility(b.getVisibility().name())
                .createdById(b.getCreatedById())
                .isClosed(b.isClosed())
                .memberCount(memberCount)
                .createdAt(b.getCreatedAt())
                .build();
    }
}