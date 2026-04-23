package com.flowboard.task.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer cardId;

    @Column(nullable = false)
    private Integer listId;

    @Column(nullable = false)
    private Integer boardId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.TO_DO;

    private LocalDate dueDate;
    private LocalDate startDate;

    private Integer assigneeId;

    @Column(nullable = false)
    private Integer createdById;

    @Column(nullable = false)
    private boolean isArchived = false;

    private String coverColor;

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

    public enum Priority {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum Status {
        TO_DO, IN_PROGRESS, IN_REVIEW, DONE
    }
}