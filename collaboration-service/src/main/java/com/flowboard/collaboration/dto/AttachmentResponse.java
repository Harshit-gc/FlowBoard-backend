package com.flowboard.collaboration.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class AttachmentResponse {
    private Integer attachmentId;
    private Integer cardId;
    private Integer uploaderId;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long sizeKb;
    private LocalDateTime uploadedAt;
}