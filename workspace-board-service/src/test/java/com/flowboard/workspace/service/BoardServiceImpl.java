package com.flowboard.workspace.service;

import com.flowboard.workspace.client.TaskServiceClient;
import com.flowboard.workspace.dto.*;
import com.flowboard.workspace.entity.Board;
import com.flowboard.workspace.entity.BoardMember;
import com.flowboard.workspace.entity.Workspace;
import com.flowboard.workspace.exception.AppException;
import com.flowboard.workspace.repository.BoardMemberRepository;
import com.flowboard.workspace.repository.BoardRepository;
import com.flowboard.workspace.repository.WorkspaceRepository;
import com.flowboard.workspace.service.impl.BoardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BoardServiceImpl Unit Tests")
class BoardServiceImplTest {

    @Mock private BoardRepository boardRepository;
    @Mock private BoardMemberRepository boardMemberRepository;
    @Mock private TaskServiceClient taskServiceClient;
    @Mock private WorkspaceRepository workspaceRepository;

    @InjectMocks private BoardServiceImpl boardService;

    // ── Shared fixtures ───────────────────────────────────────────────────────

    private Board openBoard;
    private Board closedBoard;
    private BoardMember adminMember;
    private BoardMember regularMember;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        openBoard = Board.builder()
                .boardId(1)
                .workspaceId(1)
                .name("Sprint Board")
                .description("Main sprint board")
                .background("#ffffff")
                .visibility(Board.Visibility.PRIVATE)
                .createdById(10)
                .isClosed(false)
                .build();

        closedBoard = Board.builder()
                .boardId(2)
                .workspaceId(1)
                .name("Old Board")
                .description("Archived board")
                .visibility(Board.Visibility.PRIVATE)
                .createdById(10)
                .isClosed(true)
                .build();

        adminMember = BoardMember.builder()
                .boardMemberId(1)
                .boardId(1)
                .userId(10)
                .role(BoardMember.Role.ADMIN)
                .build();

        regularMember = BoardMember.builder()
                .boardMemberId(2)
                .boardId(1)
                .userId(20)
                .role(BoardMember.Role.MEMBER)
                .build();

        workspace = Workspace.builder()
                .workspaceId(1)
                .name("My Workspace")
                .ownerId(10)
                .visibility(Workspace.Visibility.PRIVATE)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createBoard()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createBoard()")
    class CreateBoardTests {

        @Test
        @DisplayName("should create board and auto-add creator as ADMIN member")
        void createBoard_success() {
            BoardRequest req = new BoardRequest();
            req.setName("Sprint Board");
            req.setWorkspaceId(1);
            req.setVisibility("PRIVATE");

            when(boardRepository.save(any(Board.class))).thenAnswer(inv -> {
                Board b = inv.getArgument(0);
                b.setBoardId(1);
                return b;
            });
            when(boardMemberRepository.save(any())).thenReturn(adminMember);
            when(boardMemberRepository.findByBoardId(1)).thenReturn(List.of(adminMember));
            when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));

            BoardResponse resp = boardService.createBoard(req, 10);

            assertThat(resp.getName()).isEqualTo("Sprint Board");
            assertThat(resp.getCreatedById()).isEqualTo(10);
            assertThat(resp.isClosed()).isFalse();

            // Verify creator was added as ADMIN
            ArgumentCaptor<BoardMember> captor =
                    ArgumentCaptor.forClass(BoardMember.class);
            verify(boardMemberRepository).save(captor.capture());
            assertThat(captor.getValue().getRole()).isEqualTo(BoardMember.Role.ADMIN);
            assertThat(captor.getValue().getUserId()).isEqualTo(10);
        }

        @Test
        @DisplayName("should create a PUBLIC board correctly")
        void createBoard_publicVisibility() {
            BoardRequest req = new BoardRequest();
            req.setName("Public Board");
            req.setWorkspaceId(1);
            req.setVisibility("PUBLIC");

            when(boardRepository.save(any())).thenAnswer(inv -> {
                Board b = inv.getArgument(0);
                b.setBoardId(3);
                return b;
            });
            when(boardMemberRepository.save(any())).thenReturn(adminMember);
            when(boardMemberRepository.findByBoardId(3)).thenReturn(List.of());
            when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));

            boardService.createBoard(req, 10);

            ArgumentCaptor<Board> boardCaptor = ArgumentCaptor.forClass(Board.class);
            verify(boardRepository).save(boardCaptor.capture());
            assertThat(boardCaptor.getValue().getVisibility())
                    .isEqualTo(Board.Visibility.PUBLIC);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getBoardById()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getBoardById()")
    class GetBoardByIdTests {

        @Test
        @DisplayName("should return board response when found")
        void getBoardById_found() {
            when(boardRepository.findById(1)).thenReturn(Optional.of(openBoard));
            when(boardMemberRepository.findByBoardId(1)).thenReturn(List.of(adminMember));
            when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));

            BoardResponse resp = boardService.getBoardById(1);

            assertThat(resp.getBoardId()).isEqualTo(1);
            assertThat(resp.getName()).isEqualTo("Sprint Board");
            assertThat(resp.getMemberCount()).isEqualTo(1);
            assertThat(resp.getWorkspaceName()).isEqualTo("My Workspace");
        }

        @Test
        @DisplayName("should throw NOT_FOUND when board does not exist")
        void getBoardById_notFound() {
            when(boardRepository.findById(99)).thenReturn(Optional.empty());

            AppException ex = catchThrowableOfType(
                    () -> boardService.getBoardById(99), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(ex.getMessage()).isEqualTo("Board not found");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Board listing queries
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Board listing queries")
    class ListingTests {

        @Test
        @DisplayName("getBoardsByWorkspace() should return only open boards")
        void getBoardsByWorkspace_returnsOpenBoards() {
            when(boardRepository.findByWorkspaceIdAndIsClosed(1, false))
                    .thenReturn(List.of(openBoard));
            when(boardMemberRepository.findByBoardId(1)).thenReturn(List.of(adminMember));
            when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));

            List<BoardResponse> result = boardService.getBoardsByWorkspace(1);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).isClosed()).isFalse();
        }

        @Test
        @DisplayName("getClosedBoards() should return only closed boards")
        void getClosedBoards_returnsClosed() {
            when(boardRepository.findByWorkspaceIdAndIsClosed(1, true))
                    .thenReturn(List.of(closedBoard));
            when(boardMemberRepository.findByBoardId(2)).thenReturn(List.of());
            when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));

            List<BoardResponse> result = boardService.getClosedBoards(1);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).isClosed()).isTrue();
        }

        @Test
        @DisplayName("getBoardsByMember() should return boards user is member of")
        void getBoardsByMember_returnsMemberBoards() {
            when(boardMemberRepository.findBoardIdsByUserId(10)).thenReturn(List.of(1));
            when(boardRepository.findAllById(List.of(1))).thenReturn(List.of(openBoard));
            when(boardMemberRepository.findByBoardId(1)).thenReturn(List.of(adminMember));
            when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));

            List<BoardResponse> result = boardService.getBoardsByMember(10);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("getPublicBoards() should return only open PUBLIC boards")
        void getPublicBoards_returnsOpenPublic() {
            Board publicOpen = Board.builder().boardId(3).workspaceId(1)
                    .name("Public").visibility(Board.Visibility.PUBLIC)
                    .createdById(10).isClosed(false).build();
            Board publicClosed = Board.builder().boardId(4).workspaceId(1)
                    .name("Archived Public").visibility(Board.Visibility.PUBLIC)
                    .createdById(10).isClosed(true).build();

            when(boardRepository.findByVisibility(Board.Visibility.PUBLIC))
                    .thenReturn(List.of(publicOpen, publicClosed));
            when(boardMemberRepository.findByBoardId(3)).thenReturn(List.of());
            when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));

            List<BoardResponse> result = boardService.getPublicBoards();

            // closed public board should be filtered out
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Public");
        }

        @Test
        @DisplayName("getAllBoards() should return all boards")
        void getAllBoards_returnsAll() {
            when(boardRepository.findAll()).thenReturn(List.of(openBoard, closedBoard));
            when(boardMemberRepository.findByBoardId(anyInt())).thenReturn(List.of());
            when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));

            List<BoardResponse> result = boardService.getAllBoards();

            assertThat(result).hasSize(2);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateBoard()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateBoard()")
    class UpdateBoardTests {

        @Test
        @DisplayName("board admin should update board successfully")
        void updateBoard_byAdmin_success() {
            BoardRequest req = new BoardRequest();
            req.setName("Updated Board");
            req.setVisibility("PUBLIC");

            when(boardRepository.findById(1)).thenReturn(Optional.of(openBoard));
            when(boardMemberRepository.findByBoardIdAndUserId(1, 10))
                    .thenReturn(Optional.of(adminMember));
            when(boardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(boardMemberRepository.findByBoardId(1)).thenReturn(List.of(adminMember));
            when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));

            BoardResponse resp = boardService.updateBoard(1, req, 10, false);

            assertThat(resp.getName()).isEqualTo("Updated Board");
            assertThat(resp.getVisibility()).isEqualTo("PUBLIC");
        }

        @Test
        @DisplayName("PLATFORM_ADMIN should update any board")
        void updateBoard_byPlatformAdmin_success() {
            BoardRequest req = new BoardRequest();
            req.setName("Admin Updated");
            req.setVisibility("PRIVATE");

            when(boardRepository.findById(1)).thenReturn(Optional.of(openBoard));
            when(boardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(boardMemberRepository.findByBoardId(1)).thenReturn(List.of());
            when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));

            BoardResponse resp = boardService.updateBoard(1, req, 99, true);

            assertThat(resp.getName()).isEqualTo("Admin Updated");
            verify(boardMemberRepository, never()).findByBoardIdAndUserId(any(), eq(99));
        }

        @Test
        @DisplayName("non-admin non-creator should throw FORBIDDEN on update")
        void updateBoard_byStranger_throwsForbidden() {
            BoardRequest req = new BoardRequest();
            req.setName("Hacked");
            req.setVisibility("PUBLIC");

            when(boardRepository.findById(1)).thenReturn(Optional.of(openBoard));
            when(boardMemberRepository.findByBoardIdAndUserId(1, 99))
                    .thenReturn(Optional.empty());

            AppException ex = catchThrowableOfType(
                    () -> boardService.updateBoard(1, req, 99, false),
                    AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // closeBoard() / reopenBoard()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("closeBoard() and reopenBoard()")
    class ClosedBoardTests {

        @Test
        @DisplayName("should close an open board")
        void closeBoard_success() {
            when(boardRepository.findById(1)).thenReturn(Optional.of(openBoard));
            when(boardMemberRepository.findByBoardIdAndUserId(1, 10))
                    .thenReturn(Optional.of(adminMember));
            when(boardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(boardMemberRepository.findByBoardId(1)).thenReturn(List.of(adminMember));
            when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));

            BoardResponse resp = boardService.closeBoard(1, 10, false);

            assertThat(resp.isClosed()).isTrue();
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when closing an already-closed board")
        void closeBoard_alreadyClosed_throwsBadRequest() {
            when(boardRepository.findById(2)).thenReturn(Optional.of(closedBoard));
            when(boardMemberRepository.findByBoardIdAndUserId(2, 10))
                    .thenReturn(Optional.of(adminMember));

            AppException ex = catchThrowableOfType(
                    () -> boardService.closeBoard(2, 10, false),
                    AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getMessage()).isEqualTo("Board is already closed");
        }

        @Test
        @DisplayName("should reopen a closed board")
        void reopenBoard_success() {
            when(boardRepository.findById(2)).thenReturn(Optional.of(closedBoard));
            when(boardMemberRepository.findByBoardIdAndUserId(2, 10))
                    .thenReturn(Optional.of(adminMember));
            when(boardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(boardMemberRepository.findByBoardId(2)).thenReturn(List.of());
            when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));

            BoardResponse resp = boardService.reopenBoard(2, 10, false);

            assertThat(resp.isClosed()).isFalse();
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when reopening an already-open board")
        void reopenBoard_alreadyOpen_throwsBadRequest() {
            when(boardRepository.findById(1)).thenReturn(Optional.of(openBoard));
            when(boardMemberRepository.findByBoardIdAndUserId(1, 10))
                    .thenReturn(Optional.of(adminMember));

            AppException ex = catchThrowableOfType(
                    () -> boardService.reopenBoard(1, 10, false),
                    AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getMessage()).isEqualTo("Board is not closed");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteBoard()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteBoard()")
    class DeleteBoardTests {

        @Test
        @DisplayName("should delete board and all its members")
        void deleteBoard_success() {
            when(boardRepository.findById(1)).thenReturn(Optional.of(openBoard));
            when(boardMemberRepository.findByBoardIdAndUserId(1, 10))
                    .thenReturn(Optional.of(adminMember));
            when(boardMemberRepository.findByBoardId(1))
                    .thenReturn(List.of(adminMember, regularMember));

            boardService.deleteBoard(1, 10, false);

            verify(boardMemberRepository, times(2)).delete(any(BoardMember.class));
            verify(boardRepository).delete(openBoard);
        }

        @Test
        @DisplayName("PLATFORM_ADMIN should delete any board")
        void deleteBoard_byPlatformAdmin_success() {
            when(boardRepository.findById(1)).thenReturn(Optional.of(openBoard));
            when(boardMemberRepository.findByBoardId(1)).thenReturn(List.of(adminMember));

            boardService.deleteBoard(1, 99, true);

            verify(boardRepository).delete(openBoard);
        }

        @Test
        @DisplayName("non-admin should throw FORBIDDEN on delete")
        void deleteBoard_byStranger_throwsForbidden() {
            when(boardRepository.findById(1)).thenReturn(Optional.of(openBoard));
            when(boardMemberRepository.findByBoardIdAndUserId(1, 99))
                    .thenReturn(Optional.empty());

            AppException ex = catchThrowableOfType(
                    () -> boardService.deleteBoard(1, 99, false),
                    AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            verify(boardRepository, never()).delete(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // addMember() / removeMember() / updateMemberRole()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Board member management")
    class MemberManagementTests {

        @Test
        @DisplayName("addMember() should add a new board member")
        void addMember_success() {
            AddMemberRequest req = new AddMemberRequest();
            req.setUserId(30);
            req.setRole("MEMBER");

            BoardMember newMember = BoardMember.builder()
                    .boardMemberId(3).boardId(1).userId(30)
                    .role(BoardMember.Role.MEMBER).build();

            when(boardRepository.findById(1)).thenReturn(Optional.of(openBoard));
            when(boardMemberRepository.findByBoardIdAndUserId(1, 10))
                    .thenReturn(Optional.of(adminMember));
            when(boardMemberRepository.existsByBoardIdAndUserId(1, 30))
                    .thenReturn(false);
            when(boardMemberRepository.save(any())).thenReturn(newMember);

            BoardMember result = boardService.addMember(1, req, 10, false);

            assertThat(result.getUserId()).isEqualTo(30);
            assertThat(result.getRole()).isEqualTo(BoardMember.Role.MEMBER);
        }

        @Test
        @DisplayName("addMember() should throw CONFLICT when user is already a member")
        void addMember_alreadyMember_throwsConflict() {
            AddMemberRequest req = new AddMemberRequest();
            req.setUserId(20);
            req.setRole("MEMBER");

            when(boardRepository.findById(1)).thenReturn(Optional.of(openBoard));
            when(boardMemberRepository.findByBoardIdAndUserId(1, 10))
                    .thenReturn(Optional.of(adminMember));
            when(boardMemberRepository.existsByBoardIdAndUserId(1, 20))
                    .thenReturn(true);

            AppException ex = catchThrowableOfType(
                    () -> boardService.addMember(1, req, 10, false),
                    AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(ex.getMessage()).isEqualTo("User is already a board member");
        }

        @Test
        @DisplayName("removeMember() should remove a member successfully")
        void removeMember_success() {
            when(boardRepository.findById(1)).thenReturn(Optional.of(openBoard));
            when(boardMemberRepository.findByBoardIdAndUserId(1, 10))
                    .thenReturn(Optional.of(adminMember));

            boardService.removeMember(1, 20, 10, false);

            verify(boardMemberRepository).deleteByBoardIdAndUserId(1, 20);
        }

        @Test
        @DisplayName("removeMember() should throw BAD_REQUEST when removing the creator")
        void removeMember_creator_throwsBadRequest() {
            when(boardRepository.findById(1)).thenReturn(Optional.of(openBoard));
            when(boardMemberRepository.findByBoardIdAndUserId(1, 10))
                    .thenReturn(Optional.of(adminMember));

            // createdById is 10, trying to remove userId 10
            AppException ex = catchThrowableOfType(
                    () -> boardService.removeMember(1, 10, 10, false),
                    AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getMessage()).isEqualTo("Cannot remove board creator");
        }

        @Test
        @DisplayName("updateMemberRole() should update role successfully")
        void updateMemberRole_success() {
            UpdateMemberRoleRequest req = new UpdateMemberRoleRequest();
            req.setRole("ADMIN");

            when(boardRepository.findById(1)).thenReturn(Optional.of(openBoard));
            when(boardMemberRepository.findByBoardIdAndUserId(1, 10))
                    .thenReturn(Optional.of(adminMember));
            when(boardMemberRepository.findByBoardIdAndUserId(1, 20))
                    .thenReturn(Optional.of(regularMember));
            when(boardMemberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            BoardMember result = boardService.updateMemberRole(1, 20, req, 10, false);

            assertThat(result.getRole()).isEqualTo(BoardMember.Role.ADMIN);
        }

        @Test
        @DisplayName("updateMemberRole() should throw NOT_FOUND when member does not exist")
        void updateMemberRole_memberNotFound() {
            UpdateMemberRoleRequest req = new UpdateMemberRoleRequest();
            req.setRole("ADMIN");

            when(boardRepository.findById(1)).thenReturn(Optional.of(openBoard));
            when(boardMemberRepository.findByBoardIdAndUserId(1, 10))
                    .thenReturn(Optional.of(adminMember));
            when(boardMemberRepository.findByBoardIdAndUserId(1, 99))
                    .thenReturn(Optional.empty());

            AppException ex = catchThrowableOfType(
                    () -> boardService.updateMemberRole(1, 99, req, 10, false),
                    AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(ex.getMessage()).isEqualTo("Board member not found");
        }

        @Test
        @DisplayName("getMembers() should return all board members")
        void getMembers_success() {
            when(boardRepository.findById(1)).thenReturn(Optional.of(openBoard));
            when(boardMemberRepository.findByBoardId(1))
                    .thenReturn(List.of(adminMember, regularMember));

            List<BoardMember> result = boardService.getMembers(1);

            assertThat(result).hasSize(2);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getBoardAnalytics()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getBoardAnalytics()")
    class AnalyticsTests {

        @Test
        @DisplayName("should return analytics with counts from task-service")
        void getBoardAnalytics_success() {
            when(boardRepository.findById(1)).thenReturn(Optional.of(openBoard));
            when(boardMemberRepository.findByBoardId(1))
                    .thenReturn(List.of(adminMember, regularMember));
            when(taskServiceClient.getCardCountByBoard(1)).thenReturn(12L);
            when(taskServiceClient.getListCountByBoard(1)).thenReturn(3L);

            BoardAnalyticsResponse resp = boardService.getBoardAnalytics(1);

            assertThat(resp.getBoardId()).isEqualTo(1);
            assertThat(resp.getBoardName()).isEqualTo("Sprint Board");
            assertThat(resp.getTotalMembers()).isEqualTo(2);
            assertThat(resp.getTotalCards()).isEqualTo(12);
            assertThat(resp.getTotalLists()).isEqualTo(3);
            assertThat(resp.isClosed()).isFalse();
        }

        @Test
        @DisplayName("should throw NOT_FOUND for unknown board in analytics")
        void getBoardAnalytics_boardNotFound() {
            when(boardRepository.findById(99)).thenReturn(Optional.empty());

            AppException ex = catchThrowableOfType(
                    () -> boardService.getBoardAnalytics(99), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("should return zero counts when task-service returns 0")
        void getBoardAnalytics_zeroCounts() {
            when(boardRepository.findById(1)).thenReturn(Optional.of(openBoard));
            when(boardMemberRepository.findByBoardId(1)).thenReturn(List.of(adminMember));
            when(taskServiceClient.getCardCountByBoard(1)).thenReturn(0L);
            when(taskServiceClient.getListCountByBoard(1)).thenReturn(0L);

            BoardAnalyticsResponse resp = boardService.getBoardAnalytics(1);

            assertThat(resp.getTotalCards()).isEqualTo(0);
            assertThat(resp.getTotalLists()).isEqualTo(0);
        }
    }
}