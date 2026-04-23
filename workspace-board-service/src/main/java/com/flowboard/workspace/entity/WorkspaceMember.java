package com.flowboard.workspace.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "workspace_members",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"workspace_id", "user_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkspaceMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer memberId;

    @Column(nullable = false)
    private Integer workspaceId;

    @Column(nullable = false)
    private Integer userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private LocalDate joinedAt;

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDate.now();
    }

    public enum Role {
        ADMIN, MEMBER
    }
}