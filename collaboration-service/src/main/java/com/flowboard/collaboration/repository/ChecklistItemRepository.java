package com.flowboard.collaboration.repository;

import com.flowboard.collaboration.entity.ChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChecklistItemRepository
        extends JpaRepository<ChecklistItem, Integer> {

    List<ChecklistItem> findByChecklistId(Integer checklistId);
    long countByChecklistId(Integer checklistId);
    long countByChecklistIdAndIsCompleted(
            Integer checklistId, boolean isCompleted);
    void deleteByChecklistId(Integer checklistId);
}