package com.example.killertrackingportal.entity;


import jakarta.persistence.*;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "uppneda")
public class Uppneda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String phoneNumber;
    private String email;
    private String latitude;
    private String longitude;
    private String address;
    private String cityName;
    private String landSize;
    private String ownerShipStatus;
    private String landOwnerName;

    @Transient
    private List<MultipartFile> imageFile;


    private String imgPath;
}
