package com.giasuhq.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "learning_materials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "subject_name", nullable = false, length = 100)
    private String subjectName;

    @Column(name = "material_type", nullable = false, length = 50)
    @Builder.Default
    private String materialType = "pdf"; // pdf, video, exercise, quiz

    @Column(name = "type_badge", length = 50)
    @Builder.Default
    private String typeBadge = "PDF";

    @Column(name = "badge_extra", length = 50)
    private String badgeExtra; // MỚI, HOT...

    @Column(name = "author_name")
    private String authorName;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_size", length = 50)
    private String fileSize;

    @Column(name = "is_vip", nullable = false)
    @Builder.Default
    private Boolean isVip = false;

    @Column(name = "downloads_count", nullable = false)
    @Builder.Default
    private Integer downloadsCount = 0;

    @Column(name = "btn_text", length = 100)
    private String btnText;

    @Column(name = "btn_color", length = 50)
    private String btnColor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.downloadsCount == null) {
            this.downloadsCount = 0;
        }
        if (this.isVip == null) {
            this.isVip = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
