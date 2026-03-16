package com.chatbox.chatbox.repository;

import com.chatbox.chatbox.model.CollectionPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CollectionPointRepository extends JpaRepository<CollectionPoint, Long> {
    List<CollectionPoint> findByZone_Id(Long zoneId);
    List<CollectionPoint> findByStatus(String status);

    @Query("SELECT p FROM CollectionPoint p WHERE p.deleted = false AND (p.zone IS NULL OR p.zone.deleted = false)")
    List<CollectionPoint> findAllActive();

    @Query("SELECT p FROM CollectionPoint p WHERE p.zone.id = :zoneId AND p.deleted = false")
    List<CollectionPoint> findByZone_IdActive(@Param("zoneId") Long zoneId);

    @Query("SELECT p FROM CollectionPoint p WHERE p.status = :status AND p.deleted = false AND (p.zone IS NULL OR p.zone.deleted = false)")
    List<CollectionPoint> findByStatusActive(@Param("status") String status);
}
