package com.flowboard.collaboration.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "card_labels",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"card_id", "label_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CardLabel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer cardId;

    @Column(nullable = false)
    private Integer labelId;
}