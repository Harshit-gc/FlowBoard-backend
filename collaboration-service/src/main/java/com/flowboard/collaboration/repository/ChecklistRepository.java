package com.flowboard.collaboration.repository;

import com.flowboard.collaboration.entity.Checklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChecklistRepository
        extends JpaRepository<Checklist, Integer> {

    List<Checklist> findByCardIdOrderByPosition(Integer cardId);

    @Query("SELECT MAX(c.position) FROM Checklist c " +
            "WHERE c.cardId = :cardId")
    Optional<Integer> findMaxPosition(Integer cardId);
}