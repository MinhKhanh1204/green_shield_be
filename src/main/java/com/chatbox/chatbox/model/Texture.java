package com.chatbox.chatbox.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "textures")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Texture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String imageUrl;

    @Column(length = 255)
    private String name;
}
