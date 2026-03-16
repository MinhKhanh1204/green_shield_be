package com.chatbox.chatbox.model;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "collection_points")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class CollectionPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String address;

    @Column
    private Integer capacity;

    @Column
    private Integer currentStock;

    @Column(length = 255)
    private String manager;

    @Column(length = 50)
    private String phone;

    @Column
    private Double coordinatesLat;

    @Column
    private Double coordinatesLng;

    @Column(length = 20)
    private String status;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private MaterialZone zone;

    @JsonProperty("zoneId")
    public Long getZoneId() {
        return zone != null ? zone.getId() : null;
    }

    @JsonProperty("coordinates")
    public Double[] getCoordinates() {
        if (coordinatesLat != null && coordinatesLng != null) {
            return new Double[]{coordinatesLat, coordinatesLng};
        }
        return null;
    }

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @PrePersist
    public void prePersist() {
        if (deleted == null) deleted = false;
    }
}
