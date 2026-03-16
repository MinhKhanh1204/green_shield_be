package com.chatbox.chatbox.model;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Arrays;

@Entity
@Table(name = "material_zones")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MaterialZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 255)
    private String district;

    @Column(length = 255)
    private String province;

    @Column
    private Double area;

    @Column
    private Integer capacity;

    @Column(length = 20)
    private String status;

    @Column
    private Double centerLat;

    @Column
    private Double centerLng;

    @Column(columnDefinition = "TEXT")
    private String polygonData;

    @JsonIgnore
    @OneToMany(mappedBy = "zone", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Farmer> farmers;

    @JsonIgnore
    @OneToMany(mappedBy = "zone", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CollectionPoint> collectionPoints;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @PrePersist
    public void prePersist() {
        if (deleted == null) deleted = false;
    }

    @JsonProperty("center")
    public Double[] getCenter() {
        if (centerLat != null && centerLng != null) {
            return new Double[]{centerLat, centerLng};
        }
        return null;
    }

    @JsonProperty("polygon")
    public Double[][] getPolygon() {
        if (polygonData != null && !polygonData.isEmpty()) {
            try {
                String[] points = polygonData.split(";");
                Double[][] polygon = new Double[points.length][2];
                for (int i = 0; i < points.length; i++) {
                    String[] coords = points[i].split(",");
                    polygon[i][0] = Double.parseDouble(coords[0].trim());
                    polygon[i][1] = Double.parseDouble(coords[1].trim());
                }
                return polygon;
            } catch (Exception e) {
                return generateDefaultPolygon();
            }
        }
        return generateDefaultPolygon();
    }

    private Double[][] generateDefaultPolygon() {
        if (centerLat != null && centerLng != null) {
            double offset = 0.02;
            return new Double[][]{
                {centerLat - offset, centerLng - offset},
                {centerLat + offset, centerLng - offset},
                {centerLat + offset, centerLng + offset},
                {centerLat - offset, centerLng + offset}
            };
        }
        return new Double[][]{};
    }
}
