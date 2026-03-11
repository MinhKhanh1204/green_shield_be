package com.chatbox.chatbox.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "uploaded_audio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadedAudio {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false)
    private String secureUrl;

    @Column(nullable = false)
    private String publicId;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long bytes;

    @Column
    private String originalName;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = Instant.now();
    }
}

