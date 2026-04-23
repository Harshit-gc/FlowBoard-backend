package com.flowboard.workspace.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "board_members",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"board_id", "user_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BoardMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer boardMemberId;

    @Column(nullable = false)
    private Integer boardId;

    @Column(nullable = false)
    private Integer userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private LocalDate addedAt;

    @PrePersist
    protected void onCreate() {
        addedAt = LocalDate.now();
    }

    public enum Role {
        OBSERVER, MEMBER, ADMIN
    }
}