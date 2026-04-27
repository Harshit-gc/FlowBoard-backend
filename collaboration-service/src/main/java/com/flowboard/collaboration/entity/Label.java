package com.flowboard.collaboration.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "labels")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Label {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer labelId;

    // Labels are board-scoped
    @Column(nullable = false)
    private Integer boardId;

    @Column(nullable = false)
    private String name;

    // Hex color e.g. #FF5630
    @Column(nullable = false)
    private String color;

    private LocalDate createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDate.now();
    }
}