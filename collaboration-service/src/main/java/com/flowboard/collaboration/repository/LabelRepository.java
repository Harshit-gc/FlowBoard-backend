package com.flowboard.collaboration.repository;

import com.flowboard.collaboration.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LabelRepository
        extends JpaRepository<Label, Integer> {

    List<Label> findByBoardId(Integer boardId);
    boolean existsByNameAndBoardId(String name, Integer boardId);
}