package com.chatbox.chatbox.controller;

import com.chatbox.chatbox.model.MaterialZone;
import com.chatbox.chatbox.model.Farmer;
import com.chatbox.chatbox.model.CollectionPoint;
import com.chatbox.chatbox.service.MaterialZoneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/material-zones")
public class MaterialZoneController {

    @Autowired
    private MaterialZoneService materialZoneService;

    // Zone endpoints
    @GetMapping
    public ResponseEntity<List<MaterialZone>> getAllZones() {
        return ResponseEntity.ok(materialZoneService.getAllZones());
    }

    // Lấy tất cả các vùng bao gồm cả đã xóa (cho admin)
    @GetMapping("/all")
    public ResponseEntity<List<MaterialZone>> getAllZonesIncludingDeleted() {
        return ResponseEntity.ok(materialZoneService.getAllZonesIncludingDeleted());
    }

    // Khôi phục vùng đã xóa
    @PostMapping("/{id}/restore")
    public ResponseEntity<MaterialZone> restoreZone(@PathVariable Long id) {
        return ResponseEntity.ok(materialZoneService.restoreZone(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaterialZone> getZoneById(@PathVariable Long id) {
        return ResponseEntity.ok(materialZoneService.getZoneById(id));
    }

    @PostMapping
    public ResponseEntity<MaterialZone> createZone(@RequestBody MaterialZone zone) {
        return ResponseEntity.ok(materialZoneService.createZone(zone));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MaterialZone> updateZone(@PathVariable Long id, @RequestBody MaterialZone zone) {
        return ResponseEntity.ok(materialZoneService.updateZone(id, zone));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteZone(@PathVariable Long id) {
        materialZoneService.deleteZone(id);
        return ResponseEntity.ok().build();
    }

    // Farmer endpoints
    @GetMapping("/farmers")
    public ResponseEntity<List<Farmer>> getAllFarmers() {
        return ResponseEntity.ok(materialZoneService.getAllFarmers());
    }

    @GetMapping("/{zoneId}/farmers")
    public ResponseEntity<List<Farmer>> getFarmersByZoneId(@PathVariable Long zoneId) {
        return ResponseEntity.ok(materialZoneService.getFarmersByZoneId(zoneId));
    }

    @PostMapping("/farmers")
    public ResponseEntity<Farmer> createFarmer(@RequestBody Map<String, Object> request) {
        Farmer farmer = new Farmer();
        farmer.setName((String) request.get("name"));
        farmer.setPhone((String) request.get("phone"));
        farmer.setAddress((String) request.get("address"));
        farmer.setCapacity((Integer) request.get("capacity"));
        farmer.setStatus((String) request.get("status"));
        farmer.setJoinedDate((String) request.get("joinedDate"));
        if (request.get("coordinatesLat") != null) {
            farmer.setCoordinatesLat(((Number) request.get("coordinatesLat")).doubleValue());
        }
        if (request.get("coordinatesLng") != null) {
            farmer.setCoordinatesLng(((Number) request.get("coordinatesLng")).doubleValue());
        }
        Long zoneId = request.get("zoneId") != null ? ((Number) request.get("zoneId")).longValue() : null;
        return ResponseEntity.ok(materialZoneService.createFarmer(farmer, zoneId));
    }

    @PutMapping("/farmers/{id}")
    public ResponseEntity<Farmer> updateFarmer(@PathVariable Long id, @RequestBody Farmer farmer) {
        return ResponseEntity.ok(materialZoneService.updateFarmer(id, farmer));
    }

    @DeleteMapping("/farmers/{id}")
    public ResponseEntity<Void> deleteFarmer(@PathVariable Long id) {
        materialZoneService.deleteFarmer(id);
        return ResponseEntity.ok().build();
    }

    // CollectionPoint endpoints
    @GetMapping("/collection-points")
    public ResponseEntity<List<CollectionPoint>> getAllCollectionPoints() {
        return ResponseEntity.ok(materialZoneService.getAllCollectionPoints());
    }

    @GetMapping("/{zoneId}/collection-points")
    public ResponseEntity<List<CollectionPoint>> getCollectionPointsByZoneId(@PathVariable Long zoneId) {
        return ResponseEntity.ok(materialZoneService.getCollectionPointsByZoneId(zoneId));
    }

    @PostMapping("/collection-points")
    public ResponseEntity<CollectionPoint> createCollectionPoint(@RequestBody Map<String, Object> request) {
        CollectionPoint point = new CollectionPoint();
        point.setName((String) request.get("name"));
        point.setAddress((String) request.get("address"));
        point.setCapacity((Integer) request.get("capacity"));
        point.setCurrentStock((Integer) request.get("currentStock"));
        point.setManager((String) request.get("manager"));
        point.setPhone((String) request.get("phone"));
        point.setStatus((String) request.get("status"));
        if (request.get("coordinatesLat") != null) {
            point.setCoordinatesLat(((Number) request.get("coordinatesLat")).doubleValue());
        }
        if (request.get("coordinatesLng") != null) {
            point.setCoordinatesLng(((Number) request.get("coordinatesLng")).doubleValue());
        }
        Long zoneId = request.get("zoneId") != null ? ((Number) request.get("zoneId")).longValue() : null;
        return ResponseEntity.ok(materialZoneService.createCollectionPoint(point, zoneId));
    }

    @PutMapping("/collection-points/{id}")
    public ResponseEntity<CollectionPoint> updateCollectionPoint(@PathVariable Long id, @RequestBody CollectionPoint point) {
        return ResponseEntity.ok(materialZoneService.updateCollectionPoint(id, point));
    }

    @DeleteMapping("/collection-points/{id}")
    public ResponseEntity<Void> deleteCollectionPoint(@PathVariable Long id) {
        materialZoneService.deleteCollectionPoint(id);
        return ResponseEntity.ok().build();
    }

    // Stats endpoint
    @GetMapping("/stats")
    public ResponseEntity<Object> getStats() {
        return ResponseEntity.ok(materialZoneService.getStats());
    }
}
