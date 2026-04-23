package com.flowboard.task.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "card_activities")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CardActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer activityId;

    @Column(nullable = false)
    private Integer cardId;

    @Column(nullable = false)
    private Integer actorId;

    @Column(nullable = false)
    private String action;

    private String oldValue;
    private String newValue;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}