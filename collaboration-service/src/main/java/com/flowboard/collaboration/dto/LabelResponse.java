package com.flowboard.collaboration.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data @Builder
public class LabelResponse {
    private Integer labelId;
    private Integer boardId;
    private String name;
    private String color;
    private LocalDate createdAt;
}