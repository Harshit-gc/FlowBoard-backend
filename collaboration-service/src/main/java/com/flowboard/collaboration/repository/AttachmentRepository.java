package com.flowboard.collaboration.repository;

import com.flowboard.collaboration.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AttachmentRepository
        extends JpaRepository<Attachment, Integer> {

    List<Attachment> findByCardId(Integer cardId);
    List<Attachment> findByUploaderId(Integer uploaderId);
    long countByCardId(Integer cardId);
}