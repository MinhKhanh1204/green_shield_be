package com.chatbox.chatbox.repository;

import com.chatbox.chatbox.model.Farmer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FarmerRepository extends JpaRepository<Farmer, Long> {
    List<Farmer> findByZone_Id(Long zoneId);
    List<Farmer> findByStatus(String status);

    @Query("SELECT f FROM Farmer f WHERE f.deleted = false AND (f.zone IS NULL OR f.zone.deleted = false)")
    List<Farmer> findAllActive();

    @Query("SELECT f FROM Farmer f WHERE f.zone.id = :zoneId AND f.deleted = false")
    List<Farmer> findByZone_IdActive(@Param("zoneId") Long zoneId);

    @Query("SELECT f FROM Farmer f WHERE f.status = :status AND f.deleted = false AND (f.zone IS NULL OR f.zone.deleted = false)")
    List<Farmer> findByStatusActive(@Param("status") String status);
}
