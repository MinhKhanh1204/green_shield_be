package com.chatbox.chatbox.repository;

import com.chatbox.chatbox.model.BagTemplate;

import java.util.List;

public interface BagTemplateRepository extends org.springframework.data.jpa.repository.JpaRepository<BagTemplate, Long> {

    List<BagTemplate> findByActiveTrue();
}
