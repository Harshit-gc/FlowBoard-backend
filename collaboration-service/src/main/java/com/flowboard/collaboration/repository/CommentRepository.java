package com.flowboard.collaboration.repository;

import com.flowboard.collaboration.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommentRepository
        extends JpaRepository<Comment, Integer> {

    // Top-level comments only (parentCommentId is null)
    List<Comment> findByCardIdAndParentCommentIdIsNull(
            Integer cardId);

    // Replies to a specific comment
    List<Comment> findByParentCommentId(Integer parentCommentId);

    // All comments by a user
    List<Comment> findByAuthorId(Integer authorId);

    // Count comments on a card (excluding deleted)
    long countByCardIdAndIsDeleted(Integer cardId, boolean isDeleted);
}