package com.flowboard.task.repository;

import com.flowboard.task.entity.TaskList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskListRepository
        extends JpaRepository<TaskList, Integer> {

    List<TaskList> findByBoardIdOrderByPosition(Integer boardId);

    List<TaskList> findByBoardIdAndIsArchivedOrderByPosition(
            Integer boardId, boolean isArchived);

    Optional<Integer> findMaxPositionByBoardId(Integer boardId);

    @Query("SELECT MAX(t.position) FROM TaskList t WHERE t.boardId = :boardId")
    Optional<Integer> findMaxPosition(Integer boardId);

    long countByBoardId(Integer boardId);

    void deleteByListId(Integer listId);
}