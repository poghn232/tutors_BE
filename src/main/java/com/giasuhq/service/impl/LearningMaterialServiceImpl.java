package com.giasuhq.service.impl;

import com.giasuhq.dto.request.CreateMaterialRequest;
import com.giasuhq.dto.request.UpdateMaterialRequest;
import com.giasuhq.dto.response.MaterialResponse;
import com.giasuhq.entity.LearningMaterial;
import com.giasuhq.entity.Role;
import com.giasuhq.entity.User;
import com.giasuhq.repository.LearningMaterialRepository;
import com.giasuhq.service.LearningMaterialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LearningMaterialServiceImpl implements LearningMaterialService {

    private final LearningMaterialRepository materialRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    @Transactional(readOnly = true)
    public List<MaterialResponse> getAllMaterials(String subject, String type, String search) {
        String cleanSubject = (subject == null || subject.trim().isEmpty() || "all".equalsIgnoreCase(subject.trim()) || "Tất cả".equalsIgnoreCase(subject.trim())) ? null : subject.trim();
        String cleanType = (type == null || type.trim().isEmpty() || "all".equalsIgnoreCase(type.trim())) ? null : type.trim();
        String cleanSearch = (search == null || search.trim().isEmpty()) ? null : search.trim();

        List<LearningMaterial> materials = materialRepository.searchMaterials(cleanSubject, cleanType, cleanSearch);
        return materials.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MaterialResponse getMaterialById(Long id) {
        LearningMaterial material = materialRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài liệu với ID: " + id));
        return mapToResponse(material);
    }

    @Override
    @Transactional
    public MaterialResponse createMaterial(User user, CreateMaterialRequest request) {
        validateAdminOrTutor(user);

        String matType = request.getMaterialType() != null ? request.getMaterialType().toLowerCase() : "pdf";
        String typeBadge = request.getTypeBadge() != null ? request.getTypeBadge() : getTypeBadgeFor(matType);
        String btnColor = request.getBtnColor() != null ? request.getBtnColor() : getDefaultBtnColor(matType, Boolean.TRUE.equals(request.getIsVip()));
        String btnText = request.getBtnText() != null ? request.getBtnText() : getDefaultBtnText(matType, request.getFileSize());
        String author = request.getAuthorName() != null && !request.getAuthorName().isBlank() 
                ? request.getAuthorName() 
                : (user != null ? user.getFullName() : "Ban Quản Trị Tutora");

        LearningMaterial material = LearningMaterial.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : "")
                .subjectName(request.getSubjectName().trim())
                .materialType(matType)
                .typeBadge(typeBadge)
                .badgeExtra(request.getBadgeExtra())
                .authorName(author)
                .fileUrl(request.getFileUrl())
                .fileName(request.getFileName())
                .fileSize(request.getFileSize())
                .isVip(Boolean.TRUE.equals(request.getIsVip()))
                .downloadsCount(0)
                .btnText(btnText)
                .btnColor(btnColor)
                .createdBy(user)
                .build();

        LearningMaterial saved = materialRepository.save(material);
        log.info("User {} created new learning material: id={}, title={}", user != null ? user.getEmail() : "anonymous", saved.getId(), saved.getTitle());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public MaterialResponse updateMaterial(User user, Long id, UpdateMaterialRequest request) {
        validateAdminOrTutor(user);

        LearningMaterial material = materialRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài liệu với ID: " + id));

        String matType = request.getMaterialType() != null ? request.getMaterialType().toLowerCase() : material.getMaterialType();
        String typeBadge = request.getTypeBadge() != null ? request.getTypeBadge() : getTypeBadgeFor(matType);
        String btnColor = request.getBtnColor() != null ? request.getBtnColor() : getDefaultBtnColor(matType, Boolean.TRUE.equals(request.getIsVip()));
        String btnText = request.getBtnText() != null ? request.getBtnText() : getDefaultBtnText(matType, request.getFileSize() != null ? request.getFileSize() : material.getFileSize());

        material.setTitle(request.getTitle().trim());
        if (request.getDescription() != null) material.setDescription(request.getDescription().trim());
        if (request.getSubjectName() != null) material.setSubjectName(request.getSubjectName().trim());
        material.setMaterialType(matType);
        material.setTypeBadge(typeBadge);
        material.setBadgeExtra(request.getBadgeExtra());
        if (request.getAuthorName() != null && !request.getAuthorName().isBlank()) {
            material.setAuthorName(request.getAuthorName().trim());
        }
        if (request.getFileUrl() != null && !request.getFileUrl().isBlank()) {
            material.setFileUrl(request.getFileUrl());
        }
        if (request.getFileName() != null && !request.getFileName().isBlank()) {
            material.setFileName(request.getFileName());
        }
        if (request.getFileSize() != null && !request.getFileSize().isBlank()) {
            material.setFileSize(request.getFileSize());
        }
        if (request.getIsVip() != null) {
            material.setIsVip(request.getIsVip());
        }
        material.setBtnText(btnText);
        material.setBtnColor(btnColor);

        LearningMaterial updated = materialRepository.save(material);
        log.info("User {} updated learning material: id={}, title={}", user.getEmail(), updated.getId(), updated.getTitle());
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteMaterial(User user, Long id) {
        validateAdminOrTutor(user);

        LearningMaterial material = materialRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài liệu với ID: " + id));

        materialRepository.delete(material);
        log.info("User {} deleted learning material: id={}, title={}", user.getEmail(), id, material.getTitle());
    }

    @Override
    @Transactional
    public MaterialResponse incrementDownloadCount(Long id) {
        LearningMaterial material = materialRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài liệu với ID: " + id));

        material.setDownloadsCount(material.getDownloadsCount() == null ? 1 : material.getDownloadsCount() + 1);
        LearningMaterial saved = materialRepository.save(material);
        return mapToResponse(saved);
    }

    private void validateAdminOrTutor(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập để thực hiện thao tác này.");
        }
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.TUTOR) {
            throw new IllegalArgumentException("Bạn không có quyền quản lý tài liệu học tập. Yêu cầu quyền Quản Trị Viên (Admin) hoặc Gia Sư.");
        }
    }

    private MaterialResponse mapToResponse(LearningMaterial m) {
        String formattedDate = m.getCreatedAt() != null ? m.getCreatedAt().format(DATE_FORMATTER) : "01/09/2026";
        int downloads = m.getDownloadsCount() != null ? m.getDownloadsCount() : 0;
        String formattedDownloads = String.format("%,d", downloads);

        return MaterialResponse.builder()
                .id(m.getId())
                .title(m.getTitle())
                .description(m.getDescription())
                .desc(m.getDescription())
                .subjectName(m.getSubjectName())
                .subject(m.getSubjectName())
                .materialType(m.getMaterialType())
                .type(m.getMaterialType())
                .typeBadge(m.getTypeBadge() != null ? m.getTypeBadge() : getTypeBadgeFor(m.getMaterialType()))
                .badgeExtra(m.getBadgeExtra())
                .authorName(m.getAuthorName() != null ? m.getAuthorName() : "Gia Sư Tutora")
                .author(m.getAuthorName() != null ? m.getAuthorName() : "Gia Sư Tutora")
                .fileUrl(m.getFileUrl())
                .fileName(m.getFileName())
                .fileSize(m.getFileSize())
                .isVip(Boolean.TRUE.equals(m.getIsVip()))
                .downloadsCount(downloads)
                .downloads(formattedDownloads)
                .date(formattedDate)
                .btnText(m.getBtnText() != null ? m.getBtnText() : getDefaultBtnText(m.getMaterialType(), m.getFileSize()))
                .btnColor(m.getBtnColor() != null ? m.getBtnColor() : getDefaultBtnColor(m.getMaterialType(), Boolean.TRUE.equals(m.getIsVip())))
                .createdById(m.getCreatedBy() != null ? m.getCreatedBy().getId() : null)
                .createdByName(m.getCreatedBy() != null ? m.getCreatedBy().getFullName() : null)
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }

    private String getTypeBadgeFor(String type) {
        if (type == null) return "PDF";
        return switch (type.toLowerCase()) {
            case "video" -> "Video";
            case "exercise" -> "Bài tập";
            case "quiz" -> "Trắc nghiệm";
            default -> "PDF";
        };
    }

    private String getDefaultBtnColor(String type, boolean isVip) {
        if (isVip) return "#2563eb";
        if (type == null) return "#f97316";
        return switch (type.toLowerCase()) {
            case "video" -> "#7c3aed";
            case "exercise" -> "#f43f5e";
            case "quiz" -> "#00c288";
            default -> "#f97316";
        };
    }

    private String getDefaultBtnText(String type, String fileSize) {
        String sizePart = (fileSize != null && !fileSize.isBlank()) ? " (" + fileSize + ")" : "";
        if ("video".equalsIgnoreCase(type)) {
            return "Tải xuống (Video)";
        }
        if ("quiz".equalsIgnoreCase(type)) {
            return "Tải xuống";
        }
        return "Tải xuống" + sizePart;
    }
}
