package com.flowboard.workspace.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class WorkspaceResponse {
    private Integer workspaceId;
    private String name;
    private String description;
    private Integer ownerId;
    private String visibility;
    private String logoUrl;
    private Integer memberCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}