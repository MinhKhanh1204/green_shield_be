package com.chatbox.chatbox.repository;

import com.chatbox.chatbox.model.UploadedAudio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadedAudioRepository extends JpaRepository<UploadedAudio, String> {
}

