package com.chatbox.chatbox.repository;

import com.chatbox.chatbox.model.Texture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TextureRepository extends JpaRepository<Texture, Long> {

    List<Texture> findByNameContainingIgnoreCase(String name);
}
