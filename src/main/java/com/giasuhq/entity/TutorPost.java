package com.giasuhq.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tutor_posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TutorPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "grade_level", nullable = false, length = 50)
    private String gradeLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "teaching_mode", nullable = false, length = 20)
    private TeachingMode teachingMode;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String district;

    @Column(name = "detailed_address")
    private String detailedAddress;

    @Column(name = "sessions_per_week")
    @Builder.Default
    private Integer sessionsPerWeek = 2;

    @Column(name = "preferred_schedule", columnDefinition = "TEXT")
    private String preferredSchedule;

    @Column(name = "budget_amount", precision = 15, scale = 2)
    private BigDecimal budgetAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "budget_type", length = 20)
    @Builder.Default
    private BudgetType budgetType = BudgetType.PER_SESSION;

    @Enumerated(EnumType.STRING)
    @Column(name = "tutor_gender_preference", length = 20)
    @Builder.Default
    private TutorGenderPreference tutorGenderPreference = TutorGenderPreference.ANY;

    @Enumerated(EnumType.STRING)
    @Column(name = "tutor_type_preference", length = 20)
    @Builder.Default
    private TutorTypePreference tutorTypePreference = TutorTypePreference.ANY;

    @Column(name = "student_notes", columnDefinition = "TEXT")
    private String studentNotes;

    @Column(columnDefinition = "TEXT")
    private String requirements;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private TutorPostStatus status = TutorPostStatus.PENDING_APPROVAL;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = TutorPostStatus.PENDING_APPROVAL;
        }
        if (this.budgetType == null) {
            this.budgetType = BudgetType.PER_SESSION;
        }
        if (this.tutorGenderPreference == null) {
            this.tutorGenderPreference = TutorGenderPreference.ANY;
        }
        if (this.tutorTypePreference == null) {
            this.tutorTypePreference = TutorTypePreference.ANY;
        }
        if (this.sessionsPerWeek == null) {
            this.sessionsPerWeek = 2;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
