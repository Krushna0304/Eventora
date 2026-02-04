package com.Eventora.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
//Created implicity due to unique and non null constraints on email
//@Table(
//        name = "app_user",
//        indexes = {
//                @Index(name = "idx_email", columnList = "email")
//        }
//)
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String displayName;
    @Column(unique = true,nullable = false)
    private String email;
    @Column(nullable = false)
    private String password;
}
