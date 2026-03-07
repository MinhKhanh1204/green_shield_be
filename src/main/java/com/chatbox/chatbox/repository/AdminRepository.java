package com.chatbox.chatbox.repository;

import com.chatbox.chatbox.model.Admin;

import java.util.Optional;

public interface AdminRepository extends org.springframework.data.jpa.repository.JpaRepository<Admin, Long> {

    Optional<Admin> findByEmail(String email);

    boolean existsByEmail(String email);
}
