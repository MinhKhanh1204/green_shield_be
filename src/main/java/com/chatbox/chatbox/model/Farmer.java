package com.chatbox.chatbox.model;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "farmers")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Farmer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 50)
    private String phone;

    @Column(length = 500)
    private String address;

    @Column
    private Integer capacity;

    @Column(length = 20)
    private String status;

    @Column
    private String joinedDate;

    @Column
    private Double coordinatesLat;

    @Column
    private Double coordinatesLng;

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
