package com.chatbox.chatbox.repository;

import com.chatbox.chatbox.model.AiImageGenerationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;

public interface AiImageGenerationLogRepository extends JpaRepository<AiImageGenerationLog, Long> {

    @Query("SELECT COUNT(e) FROM AiImageGenerationLog e WHERE e.createdAt >= :since")
    long countSince(Instant since);
}
