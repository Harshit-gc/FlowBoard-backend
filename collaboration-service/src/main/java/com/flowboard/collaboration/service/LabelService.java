package com.flowboard.collaboration.service;

import com.flowboard.collaboration.dto.*;
import java.util.List;

public interface LabelService {

    // Label CRUD
    LabelResponse createLabel(LabelRequest request);
    LabelResponse getLabelById(Integer labelId);
    List<LabelResponse> getLabelsByBoard(Integer boardId);
    LabelResponse updateLabel(Integer labelId,
                              LabelRequest request);
    void deleteLabel(Integer labelId);

    // Card-Label association
    void addLabelToCard(Integer cardId, Integer labelId);
    void removeLabelFromCard(Integer cardId, Integer labelId);
    List<LabelResponse> getLabelsForCard(Integer cardId);

    // Checklist operations
    ChecklistResponse createChecklist(ChecklistRequest request);
    ChecklistResponse getChecklistById(Integer checklistId);
    List<ChecklistResponse> getChecklistsByCard(Integer cardId);
    void deleteChecklist(Integer checklistId);

    // Checklist item operations
    ChecklistItemResponse addItem(ChecklistItemRequest request);
    ChecklistItemResponse toggleItem(Integer itemId);
    void deleteItem(Integer itemId);
    List<ChecklistItemResponse> getItemsByChecklist(
            Integer checklistId);

    // Progress
    ChecklistProgressResponse getChecklistProgress(
            Integer checklistId);
}