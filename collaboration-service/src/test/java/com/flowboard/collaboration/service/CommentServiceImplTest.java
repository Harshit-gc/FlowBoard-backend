package com.flowboard.collaboration.service;

import com.flowboard.collaboration.client.CardClient;
import com.flowboard.collaboration.dto.*;
import com.flowboard.collaboration.entity.Attachment;
import com.flowboard.collaboration.entity.Comment;
import com.flowboard.collaboration.exception.AppException;
import com.flowboard.collaboration.messaging.NotificationPublisher;
import com.flowboard.collaboration.repository.AttachmentRepository;
import com.flowboard.collaboration.repository.CommentRepository;
import com.flowboard.collaboration.service.impl.CommentServiceImpl;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentServiceImpl Unit Tests")
class CommentServiceImplTest {

    @Mock private CommentRepository commentRepository;
    @Mock private AttachmentRepository attachmentRepository;
    @Mock private NotificationPublisher notificationPublisher;
    @Mock private CardClient cardClient;

    @InjectMocks private CommentServiceImpl commentService;

    // ── Shared fixtures ───────────────────────────────────────────────────────

    private Comment topLevelComment;
    private Comment replyComment;
    private Comment deletedComment;
    private Attachment attachment;

    @BeforeEach
    void setUp() {
        topLevelComment = Comment.builder()
                .commentId(1)
                .cardId(10)
                .authorId(100)
                .content("Great work!")
                .parentCommentId(null)
                .isDeleted(false)
                .build();

        replyComment = Comment.builder()
                .commentId(2)
                .cardId(10)
                .authorId(200)
                .content("Thanks!")
                .parentCommentId(1)
                .isDeleted(false)
                .build();

        deletedComment = Comment.builder()
                .commentId(3)
                .cardId(10)
                .authorId(100)
                .content("[deleted]")
                .parentCommentId(null)
                .isDeleted(true)
                .build();

        attachment = Attachment.builder()
                .attachmentId(1)
                .cardId(10)
                .uploaderId(100)
                .fileName("design.png")
                .fileUrl("https://cdn.example.com/design.png")
                .fileType("image/png")
                .sizeKb(512L)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // addComment()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addComment()")
    class AddCommentTests {

        @Test
        @DisplayName("should add a top-level comment and notify card owner")
        void addComment_topLevel_notifiesOwner() {
            CommentRequest req = new CommentRequest();
            req.setCardId(10);
            req.setContent("Looks good!");
            req.setParentCommentId(null);

            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
                Comment c = inv.getArgument(0);
                c.setCommentId(5);
                return c;
            });
            when(commentRepository.findByParentCommentId(5)).thenReturn(List.of());
            when(cardClient.getCardInfo(10)).thenReturn(Map.of(
                    "createdById", 200,  // owner is different from author
                    "assigneeId", -1,
                    "title", "Fix bug",
                    "boardId", 1
            ));

            // authorId = 100, cardOwnerId = 200 — should notify
            CommentResponse resp = commentService.addComment(req, 100);

            assertThat(resp.getContent()).isEqualTo("Looks good!");
            assertThat(resp.getAuthorId()).isEqualTo(100);
            assertThat(resp.getCardId()).isEqualTo(10);
            assertThat(resp.isDeleted()).isFalse();

            verify(notificationPublisher).publish(argThat(event ->
                    event.getRecipientId().equals(200) &&
                            event.getType().equals("COMMENT")));
        }

        @Test
        @DisplayName("should NOT notify when author is the card owner")
        void addComment_authorIsOwner_noNotification() {
            CommentRequest req = new CommentRequest();
            req.setCardId(10);
            req.setContent("Self comment");
            req.setParentCommentId(null);

            when(commentRepository.save(any())).thenAnswer(inv -> {
                Comment c = inv.getArgument(0);
                c.setCommentId(6);
                return c;
            });
            when(commentRepository.findByParentCommentId(6)).thenReturn(List.of());
            when(cardClient.getCardInfo(10)).thenReturn(Map.of(
                    "createdById", 100,  // same as authorId
                    "assigneeId", -1,
                    "title", "Fix bug",
                    "boardId", 1
            ));

            commentService.addComment(req, 100);

            verify(notificationPublisher, never()).publish(any());
        }

        @Test
        @DisplayName("should also notify assignee when different from author and owner")
        void addComment_notifiesAssignee() {
            CommentRequest req = new CommentRequest();
            req.setCardId(10);
            req.setContent("Hey team");
            req.setParentCommentId(null);

            when(commentRepository.save(any())).thenAnswer(inv -> {
                Comment c = inv.getArgument(0);
                c.setCommentId(7);
                return c;
            });
            when(commentRepository.findByParentCommentId(7)).thenReturn(List.of());
            when(cardClient.getCardInfo(10)).thenReturn(Map.of(
                    "createdById", 200,
                    "assigneeId", 300,  // different assignee
                    "title", "Task",
                    "boardId", 1
            ));

            // authorId=100, owner=200, assignee=300 → notify both
            commentService.addComment(req, 100);

            verify(notificationPublisher, times(2)).publish(any());
        }

        @Test
        @DisplayName("should add a reply and validate parent comment exists")
        void addComment_reply_success() {
            CommentRequest req = new CommentRequest();
            req.setCardId(10);
            req.setContent("I agree!");
            req.setParentCommentId(1);

            when(commentRepository.findById(1)).thenReturn(Optional.of(topLevelComment));
            when(commentRepository.save(any())).thenAnswer(inv -> {
                Comment c = inv.getArgument(0);
                c.setCommentId(8);
                return c;
            });
            when(commentRepository.findByParentCommentId(8)).thenReturn(List.of());
            when(cardClient.getCardInfo(10)).thenReturn(Map.of(
                    "createdById", 100,
                    "assigneeId", -1,
                    "title", "Task",
                    "boardId", 1
            ));

            CommentResponse resp = commentService.addComment(req, 200);

            assertThat(resp.getParentCommentId()).isEqualTo(1);
            // authorId=200, parent comment author=100 (different) → notify
            verify(notificationPublisher).publish(argThat(event ->
                    event.getRecipientId().equals(100)));
        }

        @Test
        @DisplayName("should throw NOT_FOUND when parent comment does not exist")
        void addComment_parentNotFound_throwsNotFound() {
            CommentRequest req = new CommentRequest();
            req.setCardId(10);
            req.setContent("Reply to ghost");
            req.setParentCommentId(999);

            when(commentRepository.findById(999)).thenReturn(Optional.empty());

            AppException ex = catchThrowableOfType(
                    () -> commentService.addComment(req, 100), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(ex.getMessage()).isEqualTo("Parent comment not found");
            verify(commentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should still save comment when cardClient call fails")
        void addComment_cardClientFails_commentSavedAnyway() {
            CommentRequest req = new CommentRequest();
            req.setCardId(10);
            req.setContent("Hello");

            when(commentRepository.save(any())).thenAnswer(inv -> {
                Comment c = inv.getArgument(0);
                c.setCommentId(9);
                return c;
            });
            when(commentRepository.findByParentCommentId(9)).thenReturn(List.of());
            when(cardClient.getCardInfo(10)).thenThrow(
                    new RuntimeException("task-service down"));

            // Should not throw — notification failure is swallowed
            assertThatNoException().isThrownBy(
                    () -> commentService.addComment(req, 100));

            verify(commentRepository).save(any());
            verify(notificationPublisher, never()).publish(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getCommentById()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getCommentById()")
    class GetCommentByIdTests {

        @Test
        @DisplayName("should return comment response when found")
        void getCommentById_found() {
            when(commentRepository.findById(1)).thenReturn(Optional.of(topLevelComment));
            when(commentRepository.findByParentCommentId(1)).thenReturn(List.of(replyComment));

            CommentResponse resp = commentService.getCommentById(1);

            assertThat(resp.getCommentId()).isEqualTo(1);
            assertThat(resp.getContent()).isEqualTo("Great work!");
            assertThat(resp.getReplyCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should throw NOT_FOUND for unknown comment")
        void getCommentById_notFound() {
            when(commentRepository.findById(99)).thenReturn(Optional.empty());

            AppException ex = catchThrowableOfType(
                    () -> commentService.getCommentById(99), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(ex.getMessage()).isEqualTo("Comment not found");
        }

        @Test
        @DisplayName("deleted comment should show [deleted] as content")
        void getCommentById_deleted_showsDeletedContent() {
            when(commentRepository.findById(3)).thenReturn(Optional.of(deletedComment));
            when(commentRepository.findByParentCommentId(3)).thenReturn(List.of());

            CommentResponse resp = commentService.getCommentById(3);

            assertThat(resp.getContent()).isEqualTo("[deleted]");
            assertThat(resp.isDeleted()).isTrue();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getCommentsByCard() / getReplies()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getCommentsByCard() and getReplies()")
    class ListingTests {

        @Test
        @DisplayName("getCommentsByCard() should return only top-level comments")
        void getCommentsByCard_returnsTopLevel() {
            when(commentRepository.findByCardIdAndParentCommentIdIsNull(10))
                    .thenReturn(List.of(topLevelComment));
            when(commentRepository.findByParentCommentId(1)).thenReturn(List.of());

            List<CommentResponse> result = commentService.getCommentsByCard(10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getParentCommentId()).isNull();
        }

        @Test
        @DisplayName("getReplies() should return all replies to a comment")
        void getReplies_success() {
            when(commentRepository.findById(1)).thenReturn(Optional.of(topLevelComment));
            when(commentRepository.findByParentCommentId(1))
                    .thenReturn(List.of(replyComment));

            List<CommentResponse> result = commentService.getReplies(1);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getParentCommentId()).isEqualTo(1);
        }

        @Test
        @DisplayName("getReplies() should throw NOT_FOUND for unknown parent")
        void getReplies_parentNotFound() {
            when(commentRepository.findById(99)).thenReturn(Optional.empty());

            AppException ex = catchThrowableOfType(
                    () -> commentService.getReplies(99), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateComment()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateComment()")
    class UpdateCommentTests {

        @Test
        @DisplayName("author should update their comment successfully")
        void updateComment_byAuthor_success() {
            when(commentRepository.findById(1)).thenReturn(Optional.of(topLevelComment));
            when(commentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(commentRepository.findByParentCommentId(1)).thenReturn(List.of());

            CommentResponse resp = commentService.updateComment(1, "Updated content", 100);

            assertThat(resp.getContent()).isEqualTo("Updated content");
        }

        @Test
        @DisplayName("should throw FORBIDDEN when non-author tries to edit")
        void updateComment_byStranger_throwsForbidden() {
            when(commentRepository.findById(1)).thenReturn(Optional.of(topLevelComment));

            AppException ex = catchThrowableOfType(
                    () -> commentService.updateComment(1, "Hacked", 999),
                    AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(ex.getMessage()).isEqualTo("Only the author can edit this comment");
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when editing a deleted comment")
        void updateComment_deleted_throwsBadRequest() {
            when(commentRepository.findById(3)).thenReturn(Optional.of(deletedComment));

            AppException ex = catchThrowableOfType(
                    () -> commentService.updateComment(3, "New content", 100),
                    AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getMessage()).isEqualTo("Cannot edit a deleted comment");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteComment()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteComment()")
    class DeleteCommentTests {

        @Test
        @DisplayName("author should soft-delete their comment")
        void deleteComment_byAuthor_softDeletes() {
            when(commentRepository.findById(1)).thenReturn(Optional.of(topLevelComment));
            when(commentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            commentService.deleteComment(1, 100);

            ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
            verify(commentRepository).save(captor.capture());
            assertThat(captor.getValue().isDeleted()).isTrue();
            assertThat(captor.getValue().getContent()).isEqualTo("[deleted]");
        }

        @Test
        @DisplayName("should throw FORBIDDEN when non-author tries to delete")
        void deleteComment_byStranger_throwsForbidden() {
            when(commentRepository.findById(1)).thenReturn(Optional.of(topLevelComment));

            AppException ex = catchThrowableOfType(
                    () -> commentService.deleteComment(1, 999), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(ex.getMessage()).isEqualTo("Only the author can delete this comment");
            verify(commentRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getCommentCount()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getCommentCount()")
    class CommentCountTests {

        @Test
        @DisplayName("should return count of non-deleted comments for a card")
        void getCommentCount_success() {
            when(commentRepository.countByCardIdAndIsDeleted(10, false)).thenReturn(5L);

            long count = commentService.getCommentCount(10);

            assertThat(count).isEqualTo(5);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // addAttachment()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addAttachment()")
    class AddAttachmentTests {

        @Test
        @DisplayName("should save and return attachment response")
        void addAttachment_success() {
            AttachmentRequest req = new AttachmentRequest();
            req.setCardId(10);
            req.setFileName("design.png");
            req.setFileUrl("https://cdn.example.com/design.png");
            req.setFileType("image/png");
            req.setSizeKb(512L);

            when(attachmentRepository.save(any(Attachment.class))).thenReturn(attachment);

            AttachmentResponse resp = commentService.addAttachment(req, 100);

            assertThat(resp.getFileName()).isEqualTo("design.png");
            assertThat(resp.getUploaderId()).isEqualTo(100);
            assertThat(resp.getCardId()).isEqualTo(10);
            assertThat(resp.getSizeKb()).isEqualTo(512L);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getAttachmentsByCard() / getAttachmentById()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAttachmentsByCard() and getAttachmentById()")
    class AttachmentQueryTests {

        @Test
        @DisplayName("getAttachmentsByCard() should return all attachments for a card")
        void getAttachmentsByCard_success() {
            when(attachmentRepository.findByCardId(10)).thenReturn(List.of(attachment));

            List<AttachmentResponse> result = commentService.getAttachmentsByCard(10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getFileName()).isEqualTo("design.png");
        }

        @Test
        @DisplayName("getAttachmentById() should return attachment when found")
        void getAttachmentById_found() {
            when(attachmentRepository.findById(1)).thenReturn(Optional.of(attachment));

            AttachmentResponse resp = commentService.getAttachmentById(1);

            assertThat(resp.getAttachmentId()).isEqualTo(1);
            assertThat(resp.getFileUrl()).isEqualTo("https://cdn.example.com/design.png");
        }

        @Test
        @DisplayName("getAttachmentById() should throw NOT_FOUND for unknown attachment")
        void getAttachmentById_notFound() {
            when(attachmentRepository.findById(99)).thenReturn(Optional.empty());

            AppException ex = catchThrowableOfType(
                    () -> commentService.getAttachmentById(99), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(ex.getMessage()).isEqualTo("Attachment not found");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteAttachment()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteAttachment()")
    class DeleteAttachmentTests {

        @Test
        @DisplayName("uploader should delete their attachment")
        void deleteAttachment_byUploader_success() {
            when(attachmentRepository.findById(1)).thenReturn(Optional.of(attachment));

            commentService.deleteAttachment(1, 100);

            verify(attachmentRepository).delete(attachment);
        }

        @Test
        @DisplayName("should throw FORBIDDEN when non-uploader tries to delete")
        void deleteAttachment_byStranger_throwsForbidden() {
            when(attachmentRepository.findById(1)).thenReturn(Optional.of(attachment));

            AppException ex = catchThrowableOfType(
                    () -> commentService.deleteAttachment(1, 999), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(ex.getMessage())
                    .isEqualTo("Only the uploader can delete this attachment");
            verify(attachmentRepository, never()).delete(any());
        }

        @Test
        @DisplayName("should throw NOT_FOUND for unknown attachment")
        void deleteAttachment_notFound() {
            when(attachmentRepository.findById(99)).thenReturn(Optional.empty());

            AppException ex = catchThrowableOfType(
                    () -> commentService.deleteAttachment(99, 100), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getAttachmentCount()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAttachmentCount()")
    class AttachmentCountTests {

        @Test
        @DisplayName("should return total attachment count for a card")
        void getAttachmentCount_success() {
            when(attachmentRepository.countByCardId(10)).thenReturn(3L);

            long count = commentService.getAttachmentCount(10);

            assertThat(count).isEqualTo(3);
        }
    }
}