package com.flowboard.collaboration.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "checklist_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer itemId;

    @Column(nullable = false)
    private Integer checklistId;

    @Column(nullable = false)
    private String text;

    @Column(nullable = false)
    private boolean isCompleted = false;

    // Optional assignee for this item
    private Integer assigneeId;

    // Optional due date for this item
    private LocalDate dueDate;
}