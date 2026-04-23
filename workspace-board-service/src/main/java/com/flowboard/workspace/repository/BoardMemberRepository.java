package com.flowboard.workspace.repository;

import com.flowboard.workspace.entity.BoardMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BoardMemberRepository
        extends JpaRepository<BoardMember, Integer> {

    List<BoardMember> findByBoardId(Integer boardId);
    List<BoardMember> findByUserId(Integer userId);
    Optional<BoardMember> findByBoardIdAndUserId(
            Integer boardId, Integer userId);
    boolean existsByBoardIdAndUserId(
            Integer boardId, Integer userId);
    void deleteByBoardIdAndUserId(
            Integer boardId, Integer userId);

    @Query("SELECT bm.boardId FROM BoardMember bm WHERE bm.userId = :userId")
    List<Integer> findBoardIdsByUserId(Integer userId);
}