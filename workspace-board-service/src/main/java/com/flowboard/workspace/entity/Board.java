package com.flowboard.workspace.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "boards")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer boardId;

    @Column(nullable = false)
    private Integer workspaceId;

    @Column(nullable = false)
    private String name;

    private String description;
    private String background;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibility visibility;

    @Column(nullable = false)
    private Integer createdById;

    @Column(nullable = false)
    private boolean isClosed = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum Visibility {
        PUBLIC, PRIVATE
    }
}