package com.chatbox.chatbox.config;

import com.chatbox.chatbox.model.MaterialZone;
import com.chatbox.chatbox.model.Farmer;
import com.chatbox.chatbox.model.CollectionPoint;
import com.chatbox.chatbox.repository.MaterialZoneRepository;
import com.chatbox.chatbox.repository.FarmerRepository;
import com.chatbox.chatbox.repository.CollectionPointRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.material-zone-seed.enabled", havingValue = "true", matchIfMissing = true)
public class MaterialZoneSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MaterialZoneSeeder.class);

    @Autowired
    private MaterialZoneRepository zoneRepository;

    @Autowired
    private FarmerRepository farmerRepository;

    @Autowired
    private CollectionPointRepository collectionPointRepository;

    @Override
    public void run(String... args) {
        if (zoneRepository.count() == 0) {
            seedData();
        }
    }

    private void seedData() {
        // Create Zones
        MaterialZone zone1 = MaterialZone.builder()
            .name("Vùng Đồng Tháp Mười")
            .district("Tam Nông, Đồng Tháp")
            .area(1250.5)
            .capacity(500)
            .status("active")
            .centerLat(10.25)
            .centerLng(105.52)
            .build();
        zone1 = zoneRepository.save(zone1);

        MaterialZone zone2 = MaterialZone.builder()
            .name("Vùng Châu Đốc")
            .district("Châu Đốc, An Giang")
            .area(890.0)
            .capacity(350)
            .status("active")
            .centerLat(10.52)
            .centerLng(105.12)
            .build();
        zone2 = zoneRepository.save(zone2);

        MaterialZone zone3 = MaterialZone.builder()
            .name("Vùng Long Xuyên")
            .district("Long Xuyên, An Giang")
            .area(650.0)
            .capacity(200)
            .status("active")
            .centerLat(10.38)
            .centerLng(105.42)
            .build();
        zone3 = zoneRepository.save(zone3);

        // Create Farmers for Zone 1
        List<Farmer> farmers1 = Arrays.asList(
            Farmer.builder()
                .name("Nguyễn Văn A")
                .phone("0912345678")
                .address("Xã Tân Phước, Tam Nông, Đồng Tháp")
                .capacity(15)
                .status("active")
                .joinedDate("2024-01-15")
                .coordinatesLat(10.22)
                .coordinatesLng(105.48)
                .zone(zone1)
                .build(),
            Farmer.builder()
                .name("Trần Thị B")
                .phone("0912345679")
                .address("Xã Tân Phước, Tam Nông, Đồng Tháp")
                .capacity(8)
                .status("active")
                .joinedDate("2024-02-20")
                .coordinatesLat(10.24)
                .coordinatesLng(105.50)
                .zone(zone1)
                .build(),
            Farmer.builder()
                .name("Lê Văn C")
                .phone("0912345680")
                .address("Xã Hòa Bình, Tam Nông, Đồng Tháp")
                .capacity(12)
                .status("active")
                .joinedDate("2024-03-10")
                .coordinatesLat(10.26)
                .coordinatesLng(105.54)
                .zone(zone1)
                .build()
        );
        farmerRepository.saveAll(farmers1);

        // Create Farmers for Zone 2
        List<Farmer> farmers2 = Arrays.asList(
            Farmer.builder()
                .name("Phạm Thị D")
                .phone("0912345681")
                .address("Xã Hòa Bình, Châu Đốc, An Giang")
                .capacity(5)
                .status("active")
                .joinedDate("2024-01-25")
                .coordinatesLat(10.50)
                .coordinatesLng(105.10)
                .zone(zone2)
                .build(),
            Farmer.builder()
                .name("Hoàng Văn E")
                .phone("0912345682")
                .address("Xã Vĩnh Nguơn, Châu Đốc, An Giang")
                .capacity(20)
                .status("active")
                .joinedDate("2024-04-05")
                .coordinatesLat(10.54)
                .coordinatesLng(105.14)
                .zone(zone2)
                .build()
        );
        farmerRepository.saveAll(farmers2);

        // Create Farmers for Zone 3
        Farmer farmer3 = Farmer.builder()
            .name("Ngô Thị F")
            .phone("0912345683")
            .address("Xã Mỹ Phú, Long Xuyên, An Giang")
            .capacity(10)
            .status("active")
            .joinedDate("2024-05-12")
            .coordinatesLat(10.36)
            .coordinatesLng(105.40)
            .zone(zone3)
            .build();
        farmerRepository.save(farmer3);

        // Create Collection Points
        CollectionPoint point1 = CollectionPoint.builder()
            .name("Kho Tam Nông")
            .address("Tam Nông, Đồng Tháp")
            .capacity(50)
            .currentStock(35)
            .manager("Mr. Tùng")
            .phone("0277382134")
            .coordinatesLat(10.25)
            .coordinatesLng(105.52)
            .status("active")
            .zone(zone1)
            .build();
        collectionPointRepository.save(point1);

        CollectionPoint point2 = CollectionPoint.builder()
            .name("Kho Châu Đốc")
            .address("Châu Đốc, An Giang")
            .capacity(30)
            .currentStock(20)
            .manager("Mr. Hùng")
            .phone("0276382123")
            .coordinatesLat(10.52)
            .coordinatesLng(105.12)
            .status("active")
            .zone(zone2)
            .build();
        collectionPointRepository.save(point2);

        CollectionPoint point3 = CollectionPoint.builder()
            .name("Kho Long Xuyên")
            .address("Long Xuyên, An Giang")
            .capacity(40)
            .currentStock(10)
            .manager("Mr. Dũng")
            .phone("0275382145")
            .coordinatesLat(10.38)
            .coordinatesLng(105.42)
            .status("active")
            .zone(zone3)
            .build();
        collectionPointRepository.save(point3);

        // Chỉ log khi chạy seed lần đầu (root logging level đang ở ERROR nên sẽ không in nhiều)
        log.info("Material Zone seed data inserted successfully!");
    }
}
