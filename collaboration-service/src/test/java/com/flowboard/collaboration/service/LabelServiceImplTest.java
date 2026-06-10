package com.flowboard.collaboration.service;

import com.flowboard.collaboration.dto.*;
import com.flowboard.collaboration.entity.*;
import com.flowboard.collaboration.exception.AppException;
import com.flowboard.collaboration.messaging.NotificationPublisher;
import com.flowboard.collaboration.repository.*;
import com.flowboard.collaboration.service.impl.LabelServiceImpl;
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
@DisplayName("LabelServiceImpl Unit Tests")
class LabelServiceImplTest {

    @Mock private LabelRepository labelRepository;
    @Mock private CardLabelRepository cardLabelRepository;
    @Mock private ChecklistRepository checklistRepository;
    @Mock private ChecklistItemRepository checklistItemRepository;
    @Mock private NotificationPublisher notificationPublisher;

    @InjectMocks private LabelServiceImpl labelService;

    // ── Shared fixtures ───────────────────────────────────────────────────────

    private Label label;
    private CardLabel cardLabel;
    private Checklist checklist;
    private ChecklistItem itemIncomplete;
    private ChecklistItem itemComplete;

    @BeforeEach
    void setUp() {
        label = Label.builder()
                .labelId(1)
                .boardId(5)
                .name("Bug")
                .color("#FF5630")
                .build();

        cardLabel = CardLabel.builder()
                .id(1)
                .cardId(10)
                .labelId(1)
                .build();

        checklist = Checklist.builder()
                .checklistId(1)
                .cardId(10)
                .title("Definition of Done")
                .position(1)
                .build();

        itemIncomplete = ChecklistItem.builder()
                .itemId(1)
                .checklistId(1)
                .text("Write tests")
                .isCompleted(false)
                .build();

        itemComplete = ChecklistItem.builder()
                .itemId(2)
                .checklistId(1)
                .text("Write code")
                .isCompleted(true)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createLabel()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createLabel()")
    class CreateLabelTests {

        @Test
        @DisplayName("should create label and return response")
        void createLabel_success() {
            LabelRequest req = new LabelRequest();
            req.setBoardId(5);
            req.setName("Bug");
            req.setColor("#FF5630");

            when(labelRepository.existsByNameAndBoardId("Bug", 5)).thenReturn(false);
            when(labelRepository.save(any(Label.class))).thenReturn(label);

            LabelResponse resp = labelService.createLabel(req);

            assertThat(resp.getLabelId()).isEqualTo(1);
            assertThat(resp.getName()).isEqualTo("Bug");
            assertThat(resp.getColor()).isEqualTo("#FF5630");
            assertThat(resp.getBoardId()).isEqualTo(5);
        }

        @Test
        @DisplayName("should throw CONFLICT when label name already exists on board")
        void createLabel_duplicateName_throwsConflict() {
            LabelRequest req = new LabelRequest();
            req.setBoardId(5);
            req.setName("Bug");
            req.setColor("#FF5630");

            when(labelRepository.existsByNameAndBoardId("Bug", 5)).thenReturn(true);

            AppException ex = catchThrowableOfType(
                    () -> labelService.createLabel(req), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(ex.getMessage())
                    .isEqualTo("Label with this name already exists on board");
            verify(labelRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getLabelById() / getLabelsByBoard()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getLabelById() and getLabelsByBoard()")
    class LabelQueryTests {

        @Test
        @DisplayName("getLabelById() should return label when found")
        void getLabelById_found() {
            when(labelRepository.findById(1)).thenReturn(Optional.of(label));

            LabelResponse resp = labelService.getLabelById(1);

            assertThat(resp.getLabelId()).isEqualTo(1);
            assertThat(resp.getName()).isEqualTo("Bug");
        }

        @Test
        @DisplayName("getLabelById() should throw NOT_FOUND for unknown label")
        void getLabelById_notFound() {
            when(labelRepository.findById(99)).thenReturn(Optional.empty());

            AppException ex = catchThrowableOfType(
                    () -> labelService.getLabelById(99), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(ex.getMessage()).isEqualTo("Label not found");
        }

        @Test
        @DisplayName("getLabelsByBoard() should return all labels for a board")
        void getLabelsByBoard_success() {
            Label label2 = Label.builder()
                    .labelId(2).boardId(5).name("Feature").color("#36B37E").build();

            when(labelRepository.findByBoardId(5)).thenReturn(List.of(label, label2));

            List<LabelResponse> result = labelService.getLabelsByBoard(5);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(LabelResponse::getName)
                    .containsExactly("Bug", "Feature");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateLabel()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateLabel()")
    class UpdateLabelTests {

        @Test
        @DisplayName("should update name and color when provided")
        void updateLabel_updatesFields() {
            LabelRequest req = new LabelRequest();
            req.setName("Critical Bug");
            req.setColor("#BF2600");

            when(labelRepository.findById(1)).thenReturn(Optional.of(label));
            when(labelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            LabelResponse resp = labelService.updateLabel(1, req);

            assertThat(resp.getName()).isEqualTo("Critical Bug");
            assertThat(resp.getColor()).isEqualTo("#BF2600");
        }

        @Test
        @DisplayName("should not overwrite fields when request values are null")
        void updateLabel_nullFieldsIgnored() {
            LabelRequest req = new LabelRequest();
            req.setName(null);
            req.setColor(null);

            when(labelRepository.findById(1)).thenReturn(Optional.of(label));
            when(labelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            LabelResponse resp = labelService.updateLabel(1, req);

            assertThat(resp.getName()).isEqualTo("Bug");
            assertThat(resp.getColor()).isEqualTo("#FF5630");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteLabel()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteLabel()")
    class DeleteLabelTests {

        @Test
        @DisplayName("should remove all card associations and delete label")
        void deleteLabel_success() {
            when(labelRepository.findById(1)).thenReturn(Optional.of(label));
            when(cardLabelRepository.findByLabelId(1)).thenReturn(List.of(cardLabel));

            labelService.deleteLabel(1);

            verify(cardLabelRepository).delete(cardLabel);
            verify(labelRepository).deleteById(1);
        }

        @Test
        @DisplayName("should throw NOT_FOUND for unknown label")
        void deleteLabel_notFound() {
            when(labelRepository.findById(99)).thenReturn(Optional.empty());

            AppException ex = catchThrowableOfType(
                    () -> labelService.deleteLabel(99), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            verify(labelRepository, never()).deleteById(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // addLabelToCard() / removeLabelFromCard() / getLabelsForCard()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Card-Label association")
    class CardLabelTests {

        @Test
        @DisplayName("addLabelToCard() should create association successfully")
        void addLabelToCard_success() {
            when(labelRepository.findById(1)).thenReturn(Optional.of(label));
            when(cardLabelRepository.existsByCardIdAndLabelId(10, 1)).thenReturn(false);
            when(cardLabelRepository.save(any())).thenReturn(cardLabel);

            assertThatNoException().isThrownBy(
                    () -> labelService.addLabelToCard(10, 1));

            ArgumentCaptor<CardLabel> captor = ArgumentCaptor.forClass(CardLabel.class);
            verify(cardLabelRepository).save(captor.capture());
            assertThat(captor.getValue().getCardId()).isEqualTo(10);
            assertThat(captor.getValue().getLabelId()).isEqualTo(1);
        }

        @Test
        @DisplayName("addLabelToCard() should throw CONFLICT when label already on card")
        void addLabelToCard_alreadyAdded_throwsConflict() {
            when(labelRepository.findById(1)).thenReturn(Optional.of(label));
            when(cardLabelRepository.existsByCardIdAndLabelId(10, 1)).thenReturn(true);

            AppException ex = catchThrowableOfType(
                    () -> labelService.addLabelToCard(10, 1), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(ex.getMessage()).isEqualTo("Label already added to this card");
            verify(cardLabelRepository, never()).save(any());
        }

        @Test
        @DisplayName("removeLabelFromCard() should delete the association")
        void removeLabelFromCard_success() {
            when(cardLabelRepository.existsByCardIdAndLabelId(10, 1)).thenReturn(true);

            assertThatNoException().isThrownBy(
                    () -> labelService.removeLabelFromCard(10, 1));

            verify(cardLabelRepository).deleteByCardIdAndLabelId(10, 1);
        }

        @Test
        @DisplayName("removeLabelFromCard() should throw NOT_FOUND when label not on card")
        void removeLabelFromCard_notFound_throwsNotFound() {
            when(cardLabelRepository.existsByCardIdAndLabelId(10, 99)).thenReturn(false);

            AppException ex = catchThrowableOfType(
                    () -> labelService.removeLabelFromCard(10, 99), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(ex.getMessage()).isEqualTo("Label not found on this card");
        }

        @Test
        @DisplayName("getLabelsForCard() should return all labels attached to a card")
        void getLabelsForCard_success() {
            when(cardLabelRepository.findByCardId(10)).thenReturn(List.of(cardLabel));
            when(labelRepository.findById(1)).thenReturn(Optional.of(label));

            List<LabelResponse> result = labelService.getLabelsForCard(10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Bug");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createChecklist() / getChecklistById() / getChecklistsByCard()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Checklist CRUD")
    class ChecklistCrudTests {

        @Test
        @DisplayName("createChecklist() should assign position = maxPosition + 1")
        void createChecklist_assignsPosition() {
            ChecklistRequest req = new ChecklistRequest();
            req.setCardId(10);
            req.setTitle("Definition of Done");

            when(checklistRepository.findMaxPosition(10)).thenReturn(Optional.of(2));
            when(checklistRepository.save(any(Checklist.class))).thenAnswer(inv -> {
                Checklist c = inv.getArgument(0);
                c.setChecklistId(1);
                return c;
            });
            when(checklistItemRepository.findByChecklistId(1)).thenReturn(List.of());

            ChecklistResponse resp = labelService.createChecklist(req);

            assertThat(resp.getTitle()).isEqualTo("Definition of Done");
            assertThat(resp.getPosition()).isEqualTo(3); // max 2 + 1
            assertThat(resp.getTotalItems()).isEqualTo(0);
            assertThat(resp.getProgressPercent()).isEqualTo(0);
        }

        @Test
        @DisplayName("createChecklist() should start at position 1 when no checklists exist")
        void createChecklist_firstChecklist_positionOne() {
            ChecklistRequest req = new ChecklistRequest();
            req.setCardId(10);
            req.setTitle("New Checklist");

            when(checklistRepository.findMaxPosition(10)).thenReturn(Optional.empty());
            when(checklistRepository.save(any())).thenAnswer(inv -> {
                Checklist c = inv.getArgument(0);
                c.setChecklistId(2);
                return c;
            });
            when(checklistItemRepository.findByChecklistId(2)).thenReturn(List.of());

            ChecklistResponse resp = labelService.createChecklist(req);

            assertThat(resp.getPosition()).isEqualTo(1);
        }

        @Test
        @DisplayName("getChecklistById() should return checklist with items and progress")
        void getChecklistById_withItems() {
            when(checklistRepository.findById(1)).thenReturn(Optional.of(checklist));
            when(checklistItemRepository.findByChecklistId(1))
                    .thenReturn(List.of(itemIncomplete, itemComplete));

            ChecklistResponse resp = labelService.getChecklistById(1);

            assertThat(resp.getChecklistId()).isEqualTo(1);
            assertThat(resp.getTotalItems()).isEqualTo(2);
            assertThat(resp.getCompletedItems()).isEqualTo(1);
            assertThat(resp.getProgressPercent()).isEqualTo(50);
        }

        @Test
        @DisplayName("getChecklistById() should throw NOT_FOUND for unknown checklist")
        void getChecklistById_notFound() {
            when(checklistRepository.findById(99)).thenReturn(Optional.empty());

            AppException ex = catchThrowableOfType(
                    () -> labelService.getChecklistById(99), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(ex.getMessage()).isEqualTo("Checklist not found");
        }

        @Test
        @DisplayName("getChecklistsByCard() should return all checklists ordered by position")
        void getChecklistsByCard_success() {
            when(checklistRepository.findByCardIdOrderByPosition(10))
                    .thenReturn(List.of(checklist));
            when(checklistItemRepository.findByChecklistId(1)).thenReturn(List.of());

            List<ChecklistResponse> result = labelService.getChecklistsByCard(10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Definition of Done");
        }

        @Test
        @DisplayName("deleteChecklist() should delete all items then the checklist")
        void deleteChecklist_success() {
            when(checklistRepository.findById(1)).thenReturn(Optional.of(checklist));

            labelService.deleteChecklist(1);

            verify(checklistItemRepository).deleteByChecklistId(1);
            verify(checklistRepository).deleteById(1);
        }

        @Test
        @DisplayName("deleteChecklist() should throw NOT_FOUND for unknown checklist")
        void deleteChecklist_notFound() {
            when(checklistRepository.findById(99)).thenReturn(Optional.empty());

            AppException ex = catchThrowableOfType(
                    () -> labelService.deleteChecklist(99), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            verify(checklistRepository, never()).deleteById(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // addItem() / toggleItem() / deleteItem() / getItemsByChecklist()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Checklist Item operations")
    class ChecklistItemTests {

        @Test
        @DisplayName("addItem() should create item with isCompleted=false")
        void addItem_success() {
            ChecklistItemRequest req = new ChecklistItemRequest();
            req.setChecklistId(1);
            req.setText("Write unit tests");
            req.setAssigneeId(100);

            when(checklistRepository.findById(1)).thenReturn(Optional.of(checklist));
            when(checklistItemRepository.save(any(ChecklistItem.class)))
                    .thenReturn(itemIncomplete);

            ChecklistItemResponse resp = labelService.addItem(req);

            assertThat(resp.getText()).isEqualTo("Write tests");
            assertThat(resp.isCompleted()).isFalse();
            assertThat(resp.getChecklistId()).isEqualTo(1);
        }

        @Test
        @DisplayName("addItem() should throw NOT_FOUND when checklist does not exist")
        void addItem_checklistNotFound() {
            ChecklistItemRequest req = new ChecklistItemRequest();
            req.setChecklistId(99);
            req.setText("Task");

            when(checklistRepository.findById(99)).thenReturn(Optional.empty());

            AppException ex = catchThrowableOfType(
                    () -> labelService.addItem(req), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            verify(checklistItemRepository, never()).save(any());
        }

        @Test
        @DisplayName("toggleItem() should flip isCompleted from false to true")
        void toggleItem_falseToTrue() {
            when(checklistItemRepository.findById(1))
                    .thenReturn(Optional.of(itemIncomplete));
            when(checklistItemRepository.save(any()))
                    .thenAnswer(inv -> inv.getArgument(0));

            ChecklistItemResponse resp = labelService.toggleItem(1);

            assertThat(resp.isCompleted()).isTrue();
        }

        @Test
        @DisplayName("toggleItem() should flip isCompleted from true to false")
        void toggleItem_trueToFalse() {
            when(checklistItemRepository.findById(2))
                    .thenReturn(Optional.of(itemComplete));
            when(checklistItemRepository.save(any()))
                    .thenAnswer(inv -> inv.getArgument(0));

            ChecklistItemResponse resp = labelService.toggleItem(2);

            assertThat(resp.isCompleted()).isFalse();
        }

        @Test
        @DisplayName("toggleItem() should throw NOT_FOUND for unknown item")
        void toggleItem_notFound() {
            when(checklistItemRepository.findById(99)).thenReturn(Optional.empty());

            AppException ex = catchThrowableOfType(
                    () -> labelService.toggleItem(99), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(ex.getMessage()).isEqualTo("Checklist item not found");
        }

        @Test
        @DisplayName("deleteItem() should delete when item exists")
        void deleteItem_success() {
            when(checklistItemRepository.findById(1))
                    .thenReturn(Optional.of(itemIncomplete));

            labelService.deleteItem(1);

            verify(checklistItemRepository).deleteById(1);
        }

        @Test
        @DisplayName("deleteItem() should throw NOT_FOUND for unknown item")
        void deleteItem_notFound() {
            when(checklistItemRepository.findById(99)).thenReturn(Optional.empty());

            AppException ex = catchThrowableOfType(
                    () -> labelService.deleteItem(99), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            verify(checklistItemRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("getItemsByChecklist() should return all items for a checklist")
        void getItemsByChecklist_success() {
            when(checklistRepository.findById(1)).thenReturn(Optional.of(checklist));
            when(checklistItemRepository.findByChecklistId(1))
                    .thenReturn(List.of(itemIncomplete, itemComplete));

            List<ChecklistItemResponse> result = labelService.getItemsByChecklist(1);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(ChecklistItemResponse::getText)
                    .containsExactly("Write tests", "Write code");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getChecklistProgress()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getChecklistProgress()")
    class ProgressTests {

        @Test
        @DisplayName("should return 50% when 1 of 2 items is completed")
        void getChecklistProgress_halfDone() {
            when(checklistRepository.findById(1)).thenReturn(Optional.of(checklist));
            when(checklistItemRepository.countByChecklistId(1)).thenReturn(2L);
            when(checklistItemRepository.countByChecklistIdAndIsCompleted(1, true))
                    .thenReturn(1L);

            ChecklistProgressResponse resp = labelService.getChecklistProgress(1);

            assertThat(resp.getChecklistId()).isEqualTo(1);
            assertThat(resp.getTotalItems()).isEqualTo(2);
            assertThat(resp.getCompletedItems()).isEqualTo(1);
            assertThat(resp.getProgressPercent()).isEqualTo(50);
        }

        @Test
        @DisplayName("should return 0% when checklist has no items")
        void getChecklistProgress_noItems_zeroPercent() {
            when(checklistRepository.findById(1)).thenReturn(Optional.of(checklist));
            when(checklistItemRepository.countByChecklistId(1)).thenReturn(0L);
            when(checklistItemRepository.countByChecklistIdAndIsCompleted(1, true))
                    .thenReturn(0L);

            ChecklistProgressResponse resp = labelService.getChecklistProgress(1);

            assertThat(resp.getProgressPercent()).isEqualTo(0);
        }

        @Test
        @DisplayName("should return 100% when all items are completed")
        void getChecklistProgress_allDone() {
            when(checklistRepository.findById(1)).thenReturn(Optional.of(checklist));
            when(checklistItemRepository.countByChecklistId(1)).thenReturn(3L);
            when(checklistItemRepository.countByChecklistIdAndIsCompleted(1, true))
                    .thenReturn(3L);

            ChecklistProgressResponse resp = labelService.getChecklistProgress(1);

            assertThat(resp.getProgressPercent()).isEqualTo(100);
        }

        @Test
        @DisplayName("should throw NOT_FOUND for unknown checklist")
        void getChecklistProgress_notFound() {
            when(checklistRepository.findById(99)).thenReturn(Optional.empty());

            AppException ex = catchThrowableOfType(
                    () -> labelService.getChecklistProgress(99), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}