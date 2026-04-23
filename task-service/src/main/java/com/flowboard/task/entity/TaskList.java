package com.flowboard.task.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_lists")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaskList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer listId;

    @Column(nullable = false)
    private Integer boardId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer position;

    private String color;

    @Column(nullable = false)
    private boolean isArchived = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}