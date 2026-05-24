package com.flowboard.task.repository;

import com.flowboard.task.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Integer> {

    List<Card> findByListIdOrderByPosition(Integer listId);

    List<Card> findByBoardIdOrderByPosition(Integer boardId);

    List<Card> findByAssigneeId(Integer assigneeId);

    List<Card> findByBoardIdAndIsArchived(
            Integer boardId, boolean isArchived);

    List<Card> findByListIdAndIsArchived(
            Integer listId, boolean isArchived);

    List<Card> findByPriority(Card.Priority priority);

    List<Card> findByStatus(Card.Status status);

    List<Card> findByBoardIdAndPriority(
            Integer boardId, Card.Priority priority);

    List<Card> findByBoardIdAndStatus(
            Integer boardId, Card.Status status);

    List<Card> findByBoardIdAndAssigneeId(
            Integer boardId, Integer assigneeId);

    @Query("SELECT MAX(c.position) FROM Card c WHERE c.listId = :listId")
    Optional<Integer> findMaxPositionByListId(Integer listId);

    @Query("SELECT c FROM Card c WHERE c.dueDate < :today " +
            "AND c.status != 'DONE' AND c.isArchived = false")
    List<Card> findOverdueCards(LocalDate today);

    @Query("SELECT c FROM Card c WHERE c.boardId = :boardId " +
            "AND c.dueDate < :today AND c.status != 'DONE' " +
            "AND c.isArchived = false")
    List<Card> findOverdueCardsByBoard(Integer boardId, LocalDate today);

    @Query("SELECT c FROM Card c WHERE " +
            "LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "AND c.boardId = :boardId AND c.isArchived = false")
    List<Card> searchByTitle(Integer boardId, String keyword);

    @Modifying
    @Query("UPDATE Card c SET c.boardId = :boardId WHERE c.listId = :listId")
    void updateBoardIdByListId(Integer listId, Integer boardId);

    List<Card> findByDueDateAndIsArchivedFalse(LocalDate dueDate);

    long countByListId(Integer listId);

    long countByBoardId(Integer boardId);

    void deleteByListId(Integer listId);
}