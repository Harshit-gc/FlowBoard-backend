package com.flowboard.task.service;

import com.flowboard.task.dto.*;
import com.flowboard.task.entity.Card;
import com.flowboard.task.entity.CardActivity;
import com.flowboard.task.exception.AppException;
import com.flowboard.task.messaging.NotificationPublisher;
import com.flowboard.task.repository.CardActivityRepository;
import com.flowboard.task.repository.CardRepository;
import com.flowboard.task.service.impl.CardServiceImpl;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CardServiceImpl Unit Tests")
class CardServiceImplTest {

    @Mock private CardRepository cardRepository;
    @Mock private CardActivityRepository activityRepository;
    @Mock private NotificationPublisher notificationPublisher;

    @InjectMocks private CardServiceImpl cardService;

    // ── Shared fixtures ───────────────────────────────────────────────────────

    private Card activeCard;
    private Card archivedCard;

    @BeforeEach
    void setUp() {
        activeCard = Card.builder()
                .cardId(1)
                .listId(10)
                .boardId(5)
                .title("Implement login")
                .description("Use JWT")
                .position(1)
                .priority(Card.Priority.MEDIUM)
                .status(Card.Status.TO_DO)
                .assigneeId(20)
                .createdById(10)
                .isArchived(false)
                .build();

        archivedCard = Card.builder()
                .cardId(2)
                .listId(10)
                .boardId(5)
                .title("Old task")
                .position(2)
                .priority(Card.Priority.LOW)
                .status(Card.Status.DONE)
                .createdById(10)
                .isArchived(true)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createCard()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createCard()")
    class CreateCardTests {

        @Test
        @DisplayName("should create card with position = maxPosition + 1 and log CREATED activity")
        void createCard_success() {
            CardRequest req = new CardRequest();
            req.setListId(10);
            req.setBoardId(5);
            req.setTitle("New Card");
            req.setPriority("HIGH");
            req.setStatus("IN_PROGRESS");

            when(cardRepository.findMaxPositionByListId(10)).thenReturn(Optional.of(2));
            when(cardRepository.save(any(Card.class))).thenAnswer(inv -> {
                Card c = inv.getArgument(0);
                c.setCardId(3);
                return c;
            });
            when(activityRepository.save(any())).thenReturn(null);

            CardResponse resp = cardService.createCard(req, 10);

            assertThat(resp.getTitle()).isEqualTo("New Card");
            assertThat(resp.getPosition()).isEqualTo(3); // max 2 + 1
            assertThat(resp.getPriority()).isEqualTo("HIGH");
            assertThat(resp.getStatus()).isEqualTo("IN_PROGRESS");

            // verify CREATED activity was logged
            ArgumentCaptor<CardActivity> actCaptor =
                    ArgumentCaptor.forClass(CardActivity.class);
            verify(activityRepository).save(actCaptor.capture());
            assertThat(actCaptor.getValue().getAction()).isEqualTo("CREATED");
        }

        @Test
        @DisplayName("should default to MEDIUM priority and TO_DO status when not provided")
        void createCard_defaultsApplied() {
            CardRequest req = new CardRequest();
            req.setListId(10);
            req.setBoardId(5);
            req.setTitle("Default Card");
            req.setPriority(null);
            req.setStatus(null);

            when(cardRepository.findMaxPositionByListId(10)).thenReturn(Optional.empty());
            when(cardRepository.save(any())).thenAnswer(inv -> {
                Card c = inv.getArgument(0);
                c.setCardId(4);
                return c;
            });
            when(activityRepository.save(any())).thenReturn(null);

            CardResponse resp = cardService.createCard(req, 10);

            assertThat(resp.getPriority()).isEqualTo("MEDIUM");
            assertThat(resp.getStatus()).isEqualTo("TO_DO");
            assertThat(resp.getPosition()).isEqualTo(1); // no existing cards
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getCardById()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getCardById()")
    class GetCardByIdTests {

        @Test
        @DisplayName("should return card response when found")
        void getCardById_found() {
            when(cardRepository.findById(1)).thenReturn(Optional.of(activeCard));

            CardResponse resp = cardService.getCardById(1);

            assertThat(resp.getCardId()).isEqualTo(1);
            assertThat(resp.getTitle()).isEqualTo("Implement login");
            assertThat(resp.isArchived()).isFalse();
        }

        @Test
        @DisplayName("should throw NOT_FOUND when card does not exist")
        void getCardById_notFound() {
            when(cardRepository.findById(99)).thenReturn(Optional.empty());

            AppException ex = catchThrowableOfType(
                    () -> cardService.getCardById(99), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(ex.getMessage()).isEqualTo("Card not found");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Card listing queries
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Card listing queries")
    class ListingTests {

        @Test
        @DisplayName("getCardsByList() should return only non-archived cards")
        void getCardsByList_filtersArchived() {
            when(cardRepository.findByListIdOrderByPosition(10))
                    .thenReturn(List.of(activeCard, archivedCard));

            List<CardResponse> result = cardService.getCardsByList(10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Implement login");
        }

        @Test
        @DisplayName("getCardsByBoard() should return only non-archived cards")
        void getCardsByBoard_filtersArchived() {
            when(cardRepository.findByBoardIdOrderByPosition(5))
                    .thenReturn(List.of(activeCard, archivedCard));

            List<CardResponse> result = cardService.getCardsByBoard(5);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("getCardsByAssignee() should return all cards for assignee")
        void getCardsByAssignee_returnsAll() {
            when(cardRepository.findByAssigneeId(20))
                    .thenReturn(List.of(activeCard));

            List<CardResponse> result = cardService.getCardsByAssignee(20);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAssigneeId()).isEqualTo(20);
        }

        @Test
        @DisplayName("getArchivedCards() should return only archived cards for a board")
        void getArchivedCards_returnsArchived() {
            when(cardRepository.findByBoardIdAndIsArchived(5, true))
                    .thenReturn(List.of(archivedCard));

            List<CardResponse> result = cardService.getArchivedCards(5);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).isArchived()).isTrue();
        }

        @Test
        @DisplayName("getAllCards() should return all cards")
        void getAllCards_returnsAll() {
            when(cardRepository.findAll()).thenReturn(List.of(activeCard, archivedCard));

            List<CardResponse> result = cardService.getAllCards();

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("searchCards() should return matching cards by keyword")
        void searchCards_returnsMatches() {
            when(cardRepository.searchByTitle(5, "login")).thenReturn(List.of(activeCard));

            List<CardResponse> result = cardService.searchCards(5, "login");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).contains("login");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateCard()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateCard()")
    class UpdateCardTests {

        @Test
        @DisplayName("should update title and log TITLE_CHANGED activity")
        void updateCard_titleChanged_logsActivity() {
            CardRequest req = new CardRequest();
            req.setTitle("Updated Title");

            when(cardRepository.findById(1)).thenReturn(Optional.of(activeCard));
            when(cardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(activityRepository.save(any())).thenReturn(null);

            CardResponse resp = cardService.updateCard(1, req, 10);

            assertThat(resp.getTitle()).isEqualTo("Updated Title");

            ArgumentCaptor<CardActivity> captor =
                    ArgumentCaptor.forClass(CardActivity.class);
            verify(activityRepository).save(captor.capture());
            assertThat(captor.getValue().getAction()).isEqualTo("TITLE_CHANGED");
            assertThat(captor.getValue().getOldValue()).isEqualTo("Implement login");
            assertThat(captor.getValue().getNewValue()).isEqualTo("Updated Title");
        }

        @Test
        @DisplayName("should update priority and log PRIORITY_CHANGED activity")
        void updateCard_priorityChanged_logsActivity() {
            CardRequest req = new CardRequest();
            req.setPriority("HIGH");

            when(cardRepository.findById(1)).thenReturn(Optional.of(activeCard));
            when(cardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(activityRepository.save(any())).thenReturn(null);

            CardResponse resp = cardService.updateCard(1, req, 10);

            assertThat(resp.getPriority()).isEqualTo("HIGH");

            ArgumentCaptor<CardActivity> captor =
                    ArgumentCaptor.forClass(CardActivity.class);
            verify(activityRepository).save(captor.capture());
            assertThat(captor.getValue().getAction()).isEqualTo("PRIORITY_CHANGED");
        }

        @Test
        @DisplayName("should send notification when due date is set and assignee is different from actor")
        void updateCard_dueDateChanged_notifiesAssignee() {
            CardRequest req = new CardRequest();
            req.setDueDate(LocalDate.now().plusDays(7));

            when(cardRepository.findById(1)).thenReturn(Optional.of(activeCard));
            when(cardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(activityRepository.save(any())).thenReturn(null);

            // actorId=10, assigneeId=20 — different, so notification should fire
            cardService.updateCard(1, req, 10);

            verify(notificationPublisher).publish(argThat(event ->
                    event.getRecipientId().equals(20) &&
                            event.getType().equals("DUE_DATE")));
        }

        @Test
        @DisplayName("should NOT send notification when actor is the assignee")
        void updateCard_dueDateChanged_noNotificationWhenActorIsAssignee() {
            CardRequest req = new CardRequest();
            req.setDueDate(LocalDate.now().plusDays(3));

            when(cardRepository.findById(1)).thenReturn(Optional.of(activeCard));
            when(cardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(activityRepository.save(any())).thenReturn(null);

            // actorId=20 same as assigneeId=20
            cardService.updateCard(1, req, 20);

            verify(notificationPublisher, never()).publish(any());
        }

        @Test
        @DisplayName("should not change fields when request values are same as current")
        void updateCard_sameValueNoActivity() {
            CardRequest req = new CardRequest();
            req.setTitle("Implement login"); // same as current
            req.setPriority("MEDIUM");       // same as current

            when(cardRepository.findById(1)).thenReturn(Optional.of(activeCard));
            when(cardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            cardService.updateCard(1, req, 10);

            // No activity logged since nothing changed
            verify(activityRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // moveCard()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("moveCard()")
    class MoveCardTests {

        @Test
        @DisplayName("should move card to target list and board and log MOVED activity")
        void moveCard_success() {
            MoveCardRequest req = new MoveCardRequest();
            req.setTargetListId(30);
            req.setTargetBoardId(6);
            req.setPosition(2);

            when(cardRepository.findById(1)).thenReturn(Optional.of(activeCard));
            when(cardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(activityRepository.save(any())).thenReturn(null);

            CardResponse resp = cardService.moveCard(1, req, 10);

            assertThat(resp.getListId()).isEqualTo(30);
            assertThat(resp.getBoardId()).isEqualTo(6);
            assertThat(resp.getPosition()).isEqualTo(2);

            ArgumentCaptor<CardActivity> captor =
                    ArgumentCaptor.forClass(CardActivity.class);
            verify(activityRepository).save(captor.capture());
            assertThat(captor.getValue().getAction()).isEqualTo("MOVED");
        }

        @Test
        @DisplayName("should notify card creator when moved by a different actor")
        void moveCard_notifiesCreator() {
            MoveCardRequest req = new MoveCardRequest();
            req.setTargetListId(30);
            req.setTargetBoardId(6);

            when(cardRepository.findById(1)).thenReturn(Optional.of(activeCard));
            when(cardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(activityRepository.save(any())).thenReturn(null);
            when(cardRepository.findMaxPositionByListId(30)).thenReturn(Optional.empty());

            // actorId=99, createdById=10 — different, should notify
            cardService.moveCard(1, req, 99);

            verify(notificationPublisher).publish(argThat(event ->
                    event.getRecipientId().equals(10) &&
                            event.getType().equals("MOVE")));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // reorderCards()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("reorderCards()")
    class ReorderCardTests {

        @Test
        @DisplayName("should update card positions in given order")
        void reorderCards_success() {
            Card card2 = Card.builder().cardId(2).listId(10)
                    .boardId(5).title("Task 2").position(2)
                    .priority(Card.Priority.LOW).status(Card.Status.TO_DO)
                    .createdById(10).isArchived(false).build();

            ReorderRequest req = new ReorderRequest();
            req.setOrderedIds(List.of(2, 1));

            when(cardRepository.findById(2)).thenReturn(Optional.of(card2));
            when(cardRepository.findById(1)).thenReturn(Optional.of(activeCard));
            when(cardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThatNoException().isThrownBy(
                    () -> cardService.reorderCards(10, req));

            ArgumentCaptor<Card> captor = ArgumentCaptor.forClass(Card.class);
            verify(cardRepository, times(2)).save(captor.capture());
            assertThat(captor.getAllValues().get(0).getPosition()).isEqualTo(1);
            assertThat(captor.getAllValues().get(1).getPosition()).isEqualTo(2);
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when card does not belong to the list")
        void reorderCards_wrongList_throwsBadRequest() {
            Card wrongListCard = Card.builder().cardId(3).listId(99)
                    .boardId(5).title("Wrong").position(1)
                    .priority(Card.Priority.LOW).status(Card.Status.TO_DO)
                    .createdById(10).isArchived(false).build();

            ReorderRequest req = new ReorderRequest();
            req.setOrderedIds(List.of(3));

            when(cardRepository.findById(3)).thenReturn(Optional.of(wrongListCard));

            AppException ex = catchThrowableOfType(
                    () -> cardService.reorderCards(10, req), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getMessage()).isEqualTo("Card does not belong to this list");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // archiveCard() / unarchiveCard()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("archiveCard() and unarchiveCard()")
    class ArchiveCardTests {

        @Test
        @DisplayName("should archive an active card and log ARCHIVED activity")
        void archiveCard_success() {
            when(cardRepository.findById(1)).thenReturn(Optional.of(activeCard));
            when(cardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(activityRepository.save(any())).thenReturn(null);

            CardResponse resp = cardService.archiveCard(1, 10);

            assertThat(resp.isArchived()).isTrue();
            ArgumentCaptor<CardActivity> captor =
                    ArgumentCaptor.forClass(CardActivity.class);
            verify(activityRepository).save(captor.capture());
            assertThat(captor.getValue().getAction()).isEqualTo("ARCHIVED");
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when archiving an already-archived card")
        void archiveCard_alreadyArchived_throwsBadRequest() {
            when(cardRepository.findById(2)).thenReturn(Optional.of(archivedCard));

            AppException ex = catchThrowableOfType(
                    () -> cardService.archiveCard(2, 10), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getMessage()).isEqualTo("Card is already archived");
        }

        @Test
        @DisplayName("should unarchive an archived card and log UNARCHIVED activity")
        void unarchiveCard_success() {
            when(cardRepository.findById(2)).thenReturn(Optional.of(archivedCard));
            when(cardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(activityRepository.save(any())).thenReturn(null);

            CardResponse resp = cardService.unarchiveCard(2, 10);

            assertThat(resp.isArchived()).isFalse();
            ArgumentCaptor<CardActivity> captor =
                    ArgumentCaptor.forClass(CardActivity.class);
            verify(activityRepository).save(captor.capture());
            assertThat(captor.getValue().getAction()).isEqualTo("UNARCHIVED");
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when unarchiving an active card")
        void unarchiveCard_notArchived_throwsBadRequest() {
            when(cardRepository.findById(1)).thenReturn(Optional.of(activeCard));

            AppException ex = catchThrowableOfType(
                    () -> cardService.unarchiveCard(1, 10), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getMessage()).isEqualTo("Card is not archived");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteCard()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteCard()")
    class DeleteCardTests {

        @Test
        @DisplayName("should delete archived card and all its activities")
        void deleteCard_success() {
            CardActivity activity = CardActivity.builder()
                    .activityId(1).cardId(2).actorId(10)
                    .action("CREATED").build();

            when(cardRepository.findById(2)).thenReturn(Optional.of(archivedCard));
            when(activityRepository.findByCardIdOrderByCreatedAtDesc(2))
                    .thenReturn(List.of(activity));

            cardService.deleteCard(2);

            verify(activityRepository).delete(activity);
            verify(cardRepository).delete(archivedCard);
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when deleting a non-archived card")
        void deleteCard_notArchived_throwsBadRequest() {
            when(cardRepository.findById(1)).thenReturn(Optional.of(activeCard));

            AppException ex = catchThrowableOfType(
                    () -> cardService.deleteCard(1), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getMessage()).isEqualTo("Archive the card before deleting");
            verify(cardRepository, never()).delete(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // setAssignee() / setPriority() / setStatus()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("setAssignee(), setPriority(), setStatus()")
    class SetterTests {

        @Test
        @DisplayName("setAssignee() should update assignee and notify new assignee")
        void setAssignee_success_notifiesNewAssignee() {
            AssigneeRequest req = new AssigneeRequest();
            req.setAssigneeId(30);

            when(cardRepository.findById(1)).thenReturn(Optional.of(activeCard));
            when(cardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(activityRepository.save(any())).thenReturn(null);

            // actorId=10, new assigneeId=30 — different, should notify
            CardResponse resp = cardService.setAssignee(1, req, 10);

            assertThat(resp.getAssigneeId()).isEqualTo(30);
            verify(notificationPublisher).publish(argThat(event ->
                    event.getRecipientId().equals(30) &&
                            event.getType().equals("ASSIGNMENT")));
        }

        @Test
        @DisplayName("setAssignee() should NOT notify when actor assigns themselves")
        void setAssignee_selfAssign_noNotification() {
            AssigneeRequest req = new AssigneeRequest();
            req.setAssigneeId(10); // same as actorId

            when(cardRepository.findById(1)).thenReturn(Optional.of(activeCard));
            when(cardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(activityRepository.save(any())).thenReturn(null);

            cardService.setAssignee(1, req, 10);

            verify(notificationPublisher, never()).publish(any());
        }

        @Test
        @DisplayName("setPriority() should update priority and notify assignee")
        void setPriority_success_notifiesAssignee() {
            PriorityRequest req = new PriorityRequest();
            req.setPriority("CRITICAL");

            when(cardRepository.findById(1)).thenReturn(Optional.of(activeCard));
            when(cardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(activityRepository.save(any())).thenReturn(null);

            CardResponse resp = cardService.setPriority(1, req, 10);

            assertThat(resp.getPriority()).isEqualTo("CRITICAL");
            // assigneeId=20, actorId=10 — different, notify
            verify(notificationPublisher).publish(argThat(event ->
                    event.getRecipientId().equals(20) &&
                            event.getType().equals("MENTION")));
        }

        @Test
        @DisplayName("setStatus() should update status and log STATUS_CHANGED activity")
        void setStatus_success() {
            StatusRequest req = new StatusRequest();
            req.setStatus("DONE");

            when(cardRepository.findById(1)).thenReturn(Optional.of(activeCard));
            when(cardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(activityRepository.save(any())).thenReturn(null);

            CardResponse resp = cardService.setStatus(1, req, 10);

            assertThat(resp.getStatus()).isEqualTo("DONE");
            ArgumentCaptor<CardActivity> captor =
                    ArgumentCaptor.forClass(CardActivity.class);
            verify(activityRepository).save(captor.capture());
            assertThat(captor.getValue().getAction()).isEqualTo("STATUS_CHANGED");
            assertThat(captor.getValue().getOldValue()).isEqualTo("TO_DO");
            assertThat(captor.getValue().getNewValue()).isEqualTo("DONE");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Overdue cards
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Overdue card queries")
    class OverdueTests {

        @Test
        @DisplayName("getOverdueCards() should return overdue cards")
        void getOverdueCards_returnsOverdue() {
            Card overdueCard = Card.builder().cardId(3).listId(10).boardId(5)
                    .title("Overdue").position(3)
                    .priority(Card.Priority.HIGH).status(Card.Status.IN_PROGRESS)
                    .dueDate(LocalDate.now().minusDays(1))
                    .createdById(10).isArchived(false).build();

            when(cardRepository.findOverdueCards(any(LocalDate.class)))
                    .thenReturn(List.of(overdueCard));

            List<CardResponse> result = cardService.getOverdueCards();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).isOverdue()).isTrue();
        }

        @Test
        @DisplayName("getOverdueCardsByBoard() should return overdue cards for a board")
        void getOverdueCardsByBoard_returnsOverdue() {
            Card overdueCard = Card.builder().cardId(3).listId(10).boardId(5)
                    .title("Overdue").position(3)
                    .priority(Card.Priority.HIGH).status(Card.Status.IN_PROGRESS)
                    .dueDate(LocalDate.now().minusDays(2))
                    .createdById(10).isArchived(false).build();

            when(cardRepository.findOverdueCardsByBoard(eq(5), any(LocalDate.class)))
                    .thenReturn(List.of(overdueCard));

            List<CardResponse> result = cardService.getOverdueCardsByBoard(5);

            assertThat(result).hasSize(1);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getCardActivity() / getCardOwnerInfo()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getCardActivity() and getCardOwnerInfo()")
    class ActivityAndOwnerTests {

        @Test
        @DisplayName("getCardActivity() should return activity log for a card")
        void getCardActivity_success() {
            CardActivity activity = CardActivity.builder()
                    .activityId(1).cardId(1).actorId(10)
                    .action("CREATED").newValue("Implement login").build();

            when(cardRepository.findById(1)).thenReturn(Optional.of(activeCard));
            when(activityRepository.findByCardIdOrderByCreatedAtDesc(1))
                    .thenReturn(List.of(activity));

            List<CardActivityResponse> result = cardService.getCardActivity(1);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAction()).isEqualTo("CREATED");
            assertThat(result.get(0).getActorId()).isEqualTo(10);
        }

        @Test
        @DisplayName("getCardOwnerInfo() should return card owner details map")
        void getCardOwnerInfo_success() {
            when(cardRepository.findById(1)).thenReturn(Optional.of(activeCard));

            Map<String, Object> info = cardService.getCardOwnerInfo(1);

            assertThat(info.get("cardId")).isEqualTo(1);
            assertThat(info.get("createdById")).isEqualTo(10);
            assertThat(info.get("assigneeId")).isEqualTo(20);
            assertThat(info.get("title")).isEqualTo("Implement login");
            assertThat(info.get("boardId")).isEqualTo(5);
        }

        @Test
        @DisplayName("getCardOwnerInfo() should return -1 for assigneeId when no assignee")
        void getCardOwnerInfo_noAssignee_returnsMinusOne() {
            activeCard.setAssigneeId(null);
            when(cardRepository.findById(1)).thenReturn(Optional.of(activeCard));

            Map<String, Object> info = cardService.getCardOwnerInfo(1);

            assertThat(info.get("assigneeId")).isEqualTo(-1);
        }
    }
}