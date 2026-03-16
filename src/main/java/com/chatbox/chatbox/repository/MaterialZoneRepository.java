package com.chatbox.chatbox.repository;

import com.chatbox.chatbox.model.MaterialZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MaterialZoneRepository extends JpaRepository<MaterialZone, Long> {
    List<MaterialZone> findByStatus(String status);

    // Tự động lọc bản ghi chưa bị xóa
    @Query("SELECT z FROM MaterialZone z WHERE z.deleted = false")
    List<MaterialZone> findAllActive();

    @Query("SELECT z FROM MaterialZone z WHERE z.id = :id AND z.deleted = false")
    Optional<MaterialZone> findByIdActive(@Param("id") Long id);

    @Query("SELECT z FROM MaterialZone z WHERE z.status = :status AND z.deleted = false")
    List<MaterialZone> findByStatusActive(@Param("status") String status);
}
