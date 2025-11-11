package com.Eventora.entity;

import com.Eventora.entity.enums.RegistrationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "event_registrations",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"event_id", "user_id"})
        },
        indexes = {
                //@Index(name = "idx_registration_event", columnList = "event_id"),
                @Index(name = "idx_registration_user", columnList = "user_id")
        }
)
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many users can register for one event
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    // Each registration belongs to a user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RegistrationStatus status = RegistrationStatus.REGISTERED;

    @CreationTimestamp
    private LocalDateTime registeredAt;

    // Optional fields
    private boolean attended = false;
    private LocalDateTime attendanceMarkedAt;

    private String paymentReference; // optional if event is paid

    // New ML/analytics fields
    @Column(precision = 10, scale = 2)
    private BigDecimal paymentAmount = BigDecimal.ZERO;

    @Column(length = 50)
    private String registrationSource; // e.g., LISTING, EMAIL, SOCIAL, REFERRAL

    @Column(length = 50)
    private String referralCode;

    private LocalDateTime viewedAt; // when user viewed listing (helps compute CTR)
    private LocalDateTime clickedAt; // when user clicked register
}
