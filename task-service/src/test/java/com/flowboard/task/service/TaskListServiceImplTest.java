package com.flowboard.task.service;

import com.flowboard.task.dto.*;
import com.flowboard.task.entity.TaskList;
import com.flowboard.task.exception.AppException;
import com.flowboard.task.repository.CardRepository;
import com.flowboard.task.repository.TaskListRepository;
import com.flowboard.task.service.impl.TaskListServiceImpl;
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
@DisplayName("TaskListServiceImpl Unit Tests")
class TaskListServiceImplTest {

    @Mock private TaskListRepository listRepository;
    @Mock private CardRepository cardRepository;

    @InjectMocks private TaskListServiceImpl taskListService;

    // ── Shared fixtures ───────────────────────────────────────────────────────

    private TaskList activeList;
    private TaskList archivedList;

    @BeforeEach
    void setUp() {
        activeList = TaskList.builder()
                .listId(1)
                .boardId(10)
                .name("To Do")
                .position(1)
                .color("#ffffff")
                .isArchived(false)
                .build();

        archivedList = TaskList.builder()
                .listId(2)
                .boardId(10)
                .name("Done")
                .position(2)
                .color("#cccccc")
                .isArchived(true)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createList()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createList()")
    class CreateListTests {

        @Test
        @DisplayName("should create list with position = maxPosition + 1")
        void createList_appendsAfterExisting() {
            TaskListRequest req = new TaskListRequest();
            req.setBoardId(10);
            req.setName("In Progress");
            req.setColor("#ff0000");

            when(listRepository.findMaxPosition(10)).thenReturn(Optional.of(2));
            when(listRepository.save(any(TaskList.class))).thenAnswer(inv -> {
                TaskList l = inv.getArgument(0);
                l.setListId(3);
                return l;
            });
            when(cardRepository.countByListId(3)).thenReturn(0L);

            TaskListResponse resp = taskListService.createList(req, 5);

            assertThat(resp.getName()).isEqualTo("In Progress");
            assertThat(resp.getPosition()).isEqualTo(3); // max 2 + 1
            assertThat(resp.getBoardId()).isEqualTo(10);
            assertThat(resp.isArchived()).isFalse();
        }

        @Test
        @DisplayName("should create list at position 1 when board has no lists")
        void createList_firstList_positionOne() {
            TaskListRequest req = new TaskListRequest();
            req.setBoardId(10);
            req.setName("First List");

            when(listRepository.findMaxPosition(10)).thenReturn(Optional.empty());
            when(listRepository.save(any(TaskList.class))).thenAnswer(inv -> {
                TaskList l = inv.getArgument(0);
                l.setListId(1);
                return l;
            });
            when(cardRepository.countByListId(1)).thenReturn(0L);

            TaskListResponse resp = taskListService.createList(req, 5);

            assertThat(resp.getPosition()).isEqualTo(1);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getListById()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getListById()")
    class GetListByIdTests {

        @Test
        @DisplayName("should return list response when found")
        void getListById_found() {
            when(listRepository.findById(1)).thenReturn(Optional.of(activeList));
            when(cardRepository.countByListId(1)).thenReturn(3L);

            TaskListResponse resp = taskListService.getListById(1);

            assertThat(resp.getListId()).isEqualTo(1);
            assertThat(resp.getName()).isEqualTo("To Do");
            assertThat(resp.getCardCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("should throw NOT_FOUND when list does not exist")
        void getListById_notFound() {
            when(listRepository.findById(99)).thenReturn(Optional.empty());

            AppException ex = catchThrowableOfType(
                    () -> taskListService.getListById(99), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(ex.getMessage()).isEqualTo("List not found");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getListsByBoard() / getArchivedLists()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getListsByBoard() and getArchivedLists()")
    class ListQueryTests {

        @Test
        @DisplayName("getListsByBoard() should return only active lists")
        void getListsByBoard_returnsActive() {
            when(listRepository.findByBoardIdAndIsArchivedOrderByPosition(10, false))
                    .thenReturn(List.of(activeList));
            when(cardRepository.countByListId(1)).thenReturn(2L);

            List<TaskListResponse> result = taskListService.getListsByBoard(10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).isArchived()).isFalse();
        }

        @Test
        @DisplayName("getArchivedLists() should return only archived lists")
        void getArchivedLists_returnsArchived() {
            when(listRepository.findByBoardIdAndIsArchivedOrderByPosition(10, true))
                    .thenReturn(List.of(archivedList));
            when(cardRepository.countByListId(2)).thenReturn(0L);

            List<TaskListResponse> result = taskListService.getArchivedLists(10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).isArchived()).isTrue();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateList()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateList()")
    class UpdateListTests {

        @Test
        @DisplayName("should update name and color when provided")
        void updateList_updatesFields() {
            TaskListRequest req = new TaskListRequest();
            req.setName("Updated Name");
            req.setColor("#123456");

            when(listRepository.findById(1)).thenReturn(Optional.of(activeList));
            when(listRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(cardRepository.countByListId(1)).thenReturn(0L);

            TaskListResponse resp = taskListService.updateList(1, req);

            assertThat(resp.getName()).isEqualTo("Updated Name");
            assertThat(resp.getColor()).isEqualTo("#123456");
        }

        @Test
        @DisplayName("should not overwrite fields when request values are null")
        void updateList_nullFieldsIgnored() {
            TaskListRequest req = new TaskListRequest();
            req.setName(null);
            req.setColor(null);

            when(listRepository.findById(1)).thenReturn(Optional.of(activeList));
            when(listRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(cardRepository.countByListId(1)).thenReturn(0L);

            TaskListResponse resp = taskListService.updateList(1, req);

            assertThat(resp.getName()).isEqualTo("To Do");
            assertThat(resp.getColor()).isEqualTo("#ffffff");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // reorderLists()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("reorderLists()")
    class ReorderListTests {

        @Test
        @DisplayName("should update positions in the given order")
        void reorderLists_success() {
            TaskList list2 = TaskList.builder()
                    .listId(2).boardId(10).name("In Progress")
                    .position(2).isArchived(false).build();

            ReorderRequest req = new ReorderRequest();
            req.setOrderedIds(List.of(2, 1)); // swap order

            when(listRepository.findById(2)).thenReturn(Optional.of(list2));
            when(listRepository.findById(1)).thenReturn(Optional.of(activeList));
            when(listRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThatNoException().isThrownBy(
                    () -> taskListService.reorderLists(10, req));

            // list2 gets position 1, list1 gets position 2
            ArgumentCaptor<TaskList> captor = ArgumentCaptor.forClass(TaskList.class);
            verify(listRepository, times(2)).save(captor.capture());
            List<TaskList> saved = captor.getAllValues();
            assertThat(saved.get(0).getListId()).isEqualTo(2);
            assertThat(saved.get(0).getPosition()).isEqualTo(1);
            assertThat(saved.get(1).getListId()).isEqualTo(1);
            assertThat(saved.get(1).getPosition()).isEqualTo(2);
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when list does not belong to the board")
        void reorderLists_wrongBoard_throwsBadRequest() {
            TaskList wrongBoardList = TaskList.builder()
                    .listId(3).boardId(99) // different board
                    .name("Foreign List").position(1).isArchived(false).build();

            ReorderRequest req = new ReorderRequest();
            req.setOrderedIds(List.of(3));

            when(listRepository.findById(3)).thenReturn(Optional.of(wrongBoardList));

            AppException ex = catchThrowableOfType(
                    () -> taskListService.reorderLists(10, req), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getMessage()).isEqualTo("List does not belong to this board");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // archiveList() / unarchiveList()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("archiveList() and unarchiveList()")
    class ArchiveTests {

        @Test
        @DisplayName("should archive an active list")
        void archiveList_success() {
            when(listRepository.findById(1)).thenReturn(Optional.of(activeList));
            when(listRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(cardRepository.countByListId(1)).thenReturn(0L);

            TaskListResponse resp = taskListService.archiveList(1);

            assertThat(resp.isArchived()).isTrue();
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when archiving an already-archived list")
        void archiveList_alreadyArchived_throwsBadRequest() {
            when(listRepository.findById(2)).thenReturn(Optional.of(archivedList));

            AppException ex = catchThrowableOfType(
                    () -> taskListService.archiveList(2), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getMessage()).isEqualTo("List is already archived");
        }

        @Test
        @DisplayName("should unarchive an archived list")
        void unarchiveList_success() {
            when(listRepository.findById(2)).thenReturn(Optional.of(archivedList));
            when(listRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(cardRepository.countByListId(2)).thenReturn(0L);

            TaskListResponse resp = taskListService.unarchiveList(2);

            assertThat(resp.isArchived()).isFalse();
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when unarchiving an active list")
        void unarchiveList_notArchived_throwsBadRequest() {
            when(listRepository.findById(1)).thenReturn(Optional.of(activeList));

            AppException ex = catchThrowableOfType(
                    () -> taskListService.unarchiveList(1), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getMessage()).isEqualTo("List is not archived");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteList()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteList()")
    class DeleteListTests {

        @Test
        @DisplayName("should delete archived list and all its cards")
        void deleteList_success() {
            when(listRepository.findById(2)).thenReturn(Optional.of(archivedList));

            taskListService.deleteList(2);

            verify(cardRepository).deleteByListId(2);
            verify(listRepository).delete(archivedList);
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when deleting a non-archived list")
        void deleteList_notArchived_throwsBadRequest() {
            when(listRepository.findById(1)).thenReturn(Optional.of(activeList));

            AppException ex = catchThrowableOfType(
                    () -> taskListService.deleteList(1), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getMessage()).isEqualTo("Archive the list before deleting");
            verify(listRepository, never()).delete(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // moveList()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("moveList()")
    class MoveListTests {

        @Test
        @DisplayName("should move list to a target board and assign next position")
        void moveList_success() {
            MoveListRequest req = new MoveListRequest();
            req.setTargetBoardId(20);

            when(listRepository.findById(1)).thenReturn(Optional.of(activeList));
            when(listRepository.findMaxPosition(20)).thenReturn(Optional.of(3));
            when(listRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(cardRepository.countByListId(1)).thenReturn(0L);

            TaskListResponse resp = taskListService.moveList(1, req);

            assertThat(resp.getBoardId()).isEqualTo(20);
            assertThat(resp.getPosition()).isEqualTo(4); // 3 + 1
            verify(cardRepository).updateBoardIdByListId(1, 20);
        }

        @Test
        @DisplayName("should place at position 1 when target board has no lists")
        void moveList_emptyTargetBoard_positionOne() {
            MoveListRequest req = new MoveListRequest();
            req.setTargetBoardId(20);

            when(listRepository.findById(1)).thenReturn(Optional.of(activeList));
            when(listRepository.findMaxPosition(20)).thenReturn(Optional.empty());
            when(listRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(cardRepository.countByListId(1)).thenReturn(0L);

            TaskListResponse resp = taskListService.moveList(1, req);

            assertThat(resp.getPosition()).isEqualTo(1);
        }
    }
}