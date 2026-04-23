package com.flowboard.task.repository;

import com.flowboard.task.entity.CardActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CardActivityRepository
        extends JpaRepository<CardActivity, Integer> {

    List<CardActivity> findByCardIdOrderByCreatedAtDesc(Integer cardId);
    List<CardActivity> findByActorId(Integer actorId);
}