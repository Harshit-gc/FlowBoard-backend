package com.flowboard.workspace.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class BoardResponse {
    private Integer boardId;
    private Integer workspaceId;
    private String name;
    private String description;
    private String background;
    private String visibility;
    private Integer createdById;
    private boolean isClosed;
    private Integer memberCount;
    private LocalDateTime createdAt;
}