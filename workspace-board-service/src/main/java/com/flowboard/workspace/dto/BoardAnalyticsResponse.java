package com.flowboard.workspace.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class BoardAnalyticsResponse {
    private Integer boardId;
    private String boardName;
    private Integer totalCards;
    private Integer totalMembers;
    private Integer totalLists;
    private boolean isClosed;
}