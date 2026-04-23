package com.flowboard.workspace.repository;

import com.flowboard.workspace.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BoardRepository extends JpaRepository<Board, Integer> {

    List<Board> findByWorkspaceId(Integer workspaceId);
    List<Board> findByCreatedById(Integer createdById);
    List<Board> findByVisibility(Board.Visibility visibility);
    List<Board> findByIsClosed(boolean isClosed);
    List<Board> findByWorkspaceIdAndIsClosed(
            Integer workspaceId, boolean isClosed);
    long countByWorkspaceId(Integer workspaceId);
}