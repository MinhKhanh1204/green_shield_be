package com.chatbox.chatbox.repository;

import com.chatbox.chatbox.model.AiAudioGenerationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;

public interface AiAudioGenerationLogRepository extends JpaRepository<AiAudioGenerationLog, Long> {

    @Query("SELECT COUNT(e) FROM AiAudioGenerationLog e WHERE e.createdAt >= :since")
    long countSince(Instant since);
}

