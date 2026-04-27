package com.flowboard.collaboration.repository;

import com.flowboard.collaboration.entity.CardLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CardLabelRepository
        extends JpaRepository<CardLabel, Integer> {

    List<CardLabel> findByCardId(Integer cardId);
    List<CardLabel> findByLabelId(Integer labelId);
    Optional<CardLabel> findByCardIdAndLabelId(
            Integer cardId, Integer labelId);
    boolean existsByCardIdAndLabelId(
            Integer cardId, Integer labelId);
    void deleteByCardIdAndLabelId(
            Integer cardId, Integer labelId);
}