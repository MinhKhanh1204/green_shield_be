package com.chatbox.chatbox.service;

import com.chatbox.chatbox.model.MaterialZone;
import com.chatbox.chatbox.model.Farmer;
import com.chatbox.chatbox.model.CollectionPoint;
import com.chatbox.chatbox.repository.MaterialZoneRepository;
import com.chatbox.chatbox.repository.FarmerRepository;
import com.chatbox.chatbox.repository.CollectionPointRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class MaterialZoneService {

    @Autowired
    private MaterialZoneRepository zoneRepository;

    @Autowired
    private FarmerRepository farmerRepository;

    @Autowired
    private CollectionPointRepository collectionPointRepository;

    // Zone operations
    public List<MaterialZone> getAllZones() {
        return zoneRepository.findAllActive();
    }

    public MaterialZone getZoneById(Long id) {
        return zoneRepository.findByIdActive(id).orElse(null);
    }

    @Transactional
    public MaterialZone createZone(MaterialZone zone) {
        if (zone.getDeleted() == null) {
            zone.setDeleted(false);
        }
        return zoneRepository.save(zone);
    }

    @Transactional
    public MaterialZone updateZone(Long id, MaterialZone zone) {
        MaterialZone existing = zoneRepository.findByIdActive(id).orElse(null);
        if (existing != null) {
            existing.setName(zone.getName());
            existing.setDistrict(zone.getDistrict());
            existing.setProvince(zone.getProvince());
            existing.setArea(zone.getArea());
            existing.setCapacity(zone.getCapacity());
            existing.setStatus(zone.getStatus());
            existing.setCenterLat(zone.getCenterLat());
            existing.setCenterLng(zone.getCenterLng());
            existing.setPolygonData(zone.getPolygonData());
            return zoneRepository.save(existing);
        }
        return null;
    }

    @Transactional
    public void deleteZone(Long id) {
        zoneRepository.findByIdActive(id).ifPresent(zone -> {
            zone.setDeleted(true);
            zoneRepository.save(zone);
        });
    }

    // Khôi phục vùng đã xóa (nếu cần)
    @Transactional
    public MaterialZone restoreZone(Long id) {
        return zoneRepository.findById(id).map(zone -> {
            zone.setDeleted(false);
            return zoneRepository.save(zone);
        }).orElse(null);
    }

    // Lấy tất cả các vùng bao gồm cả đã xóa (cho admin)
    public List<MaterialZone> getAllZonesIncludingDeleted() {
        return zoneRepository.findAll();
    }

    // Farmer operations
    public List<Farmer> getAllFarmers() {
        return farmerRepository.findAllActive();
    }

    public List<Farmer> getFarmersByZoneId(Long zoneId) {
        return farmerRepository.findByZone_IdActive(zoneId);
    }

    @Transactional
    public Farmer createFarmer(Farmer farmer, Long zoneId) {
        MaterialZone zone = zoneRepository.findByIdActive(zoneId).orElse(null);
        if (zone != null) {
            farmer.setZone(zone);
        }
        return farmerRepository.save(farmer);
    }

    @Transactional
    public Farmer updateFarmer(Long id, Farmer farmer) {
        Farmer existing = farmerRepository.findById(id).orElse(null);
        if (existing != null && !existing.getDeleted()) {
            existing.setName(farmer.getName());
            existing.setPhone(farmer.getPhone());
            existing.setAddress(farmer.getAddress());
            existing.setCapacity(farmer.getCapacity());
            existing.setStatus(farmer.getStatus());
            existing.setJoinedDate(farmer.getJoinedDate());
            existing.setCoordinatesLat(farmer.getCoordinatesLat());
            existing.setCoordinatesLng(farmer.getCoordinatesLng());
            return farmerRepository.save(existing);
        }
        return null;
    }

    @Transactional
    public void deleteFarmer(Long id) {
        farmerRepository.findById(id).ifPresent(farmer -> {
            farmer.setDeleted(true);
            farmerRepository.save(farmer);
        });
    }

    // CollectionPoint operations
    public List<CollectionPoint> getAllCollectionPoints() {
        return collectionPointRepository.findAllActive();
    }

    public List<CollectionPoint> getCollectionPointsByZoneId(Long zoneId) {
        return collectionPointRepository.findByZone_IdActive(zoneId);
    }

    @Transactional
    public CollectionPoint createCollectionPoint(CollectionPoint point, Long zoneId) {
        MaterialZone zone = zoneRepository.findByIdActive(zoneId).orElse(null);
        if (zone != null) {
            point.setZone(zone);
        }
        return collectionPointRepository.save(point);
    }

    @Transactional
    public CollectionPoint updateCollectionPoint(Long id, CollectionPoint point) {
        CollectionPoint existing = collectionPointRepository.findById(id).orElse(null);
        if (existing != null && !existing.getDeleted()) {
            existing.setName(point.getName());
            existing.setAddress(point.getAddress());
            existing.setCapacity(point.getCapacity());
            existing.setCurrentStock(point.getCurrentStock());
            existing.setManager(point.getManager());
            existing.setPhone(point.getPhone());
            existing.setCoordinatesLat(point.getCoordinatesLat());
            existing.setCoordinatesLng(point.getCoordinatesLng());
            existing.setStatus(point.getStatus());
            return collectionPointRepository.save(existing);
        }
        return null;
    }

    @Transactional
    public void deleteCollectionPoint(Long id) {
        collectionPointRepository.findById(id).ifPresent(point -> {
            point.setDeleted(true);
            collectionPointRepository.save(point);
        });
    }

    // Statistics
    public Map<String, Object> getStats() {
        List<MaterialZone> zones = zoneRepository.findAllActive();
        List<Farmer> farmers = farmerRepository.findAllActive();
        List<CollectionPoint> points = collectionPointRepository.findAllActive();

        int activeZonesCount = (int) zones.stream().filter(z -> "active".equals(z.getStatus())).count();
        int activeFarmersCount = (int) farmers.stream().filter(f -> "active".equals(f.getStatus())).count();
        int activePointsCount = (int) points.stream().filter(p -> "active".equals(p.getStatus())).count();
        int totalCapacitySum = zones.stream().mapToInt(z -> z.getCapacity() != null ? z.getCapacity() : 0).sum();
        int totalFarmerCapacitySum = farmers.stream().mapToInt(f -> f.getCapacity() != null ? f.getCapacity() : 0).sum();
        int totalStockSum = points.stream().mapToInt(p -> p.getCurrentStock() != null ? p.getCurrentStock() : 0).sum();
        int totalStockCapacitySum = points.stream().mapToInt(p -> p.getCapacity() != null ? p.getCapacity() : 0).sum();

        return Map.of(
            "totalZones", zones.size(),
            "activeZones", activeZonesCount,
            "totalCapacity", totalCapacitySum,
            "totalFarmers", farmers.size(),
            "activeFarmers", activeFarmersCount,
            "totalFarmerCapacity", totalFarmerCapacitySum,
            "totalCollectionPoints", points.size(),
            "activePoints", activePointsCount,
            "totalStock", totalStockSum,
            "totalStockCapacity", totalStockCapacitySum
        );
    }
}
