package com.giasuhq.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;

@Entity
@Table(name = "tutors")
@PrimaryKeyJoinColumn(name = "user_id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Tutor extends User {

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String qualification;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "tutor_subjects",
        joinColumns = @JoinColumn(name = "tutor_id"),
        inverseJoinColumns = @JoinColumn(name = "subject_id")
    )
    @Builder.Default
    private java.util.Set<Subject> subjects = new java.util.HashSet<>();

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "facebook_url")
    private String facebookUrl;

    @Column(name = "verification_status", nullable = false)
    @Builder.Default
    private String verificationStatus = "PENDING"; // PENDING, APPROVED, REJECTED

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "certificates_json", columnDefinition = "LONGTEXT")
    private String certificatesJson;

    @Column(name = "available_days_json", columnDefinition = "TEXT")
    private String availableDaysJson;

    @Column(name = "available_times_json", columnDefinition = "TEXT")
    private String availableTimesJson;
}

