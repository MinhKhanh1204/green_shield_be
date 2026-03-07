package com.chatbox.chatbox.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bag_templates")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class BagTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private Double basePrice;

    @Column(nullable = false, length = 500)
    private String frontImageUrl;

    @Column(nullable = false, length = 500)
    private String frontCustomArea; // JSON: {x, y, width, height} in %

    @Column(nullable = false, length = 500)
    private String backImageUrl;

    @Column(nullable = false, length = 500)
    private String backCustomArea; // JSON: {x, y, width, height} in %

    @Column(columnDefinition = "TEXT")
    private String previewConfig; // JSON: [{imageUrl, customArea: {x,y,width,height}}]

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
