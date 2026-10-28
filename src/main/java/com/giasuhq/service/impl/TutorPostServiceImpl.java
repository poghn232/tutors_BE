package com.giasuhq.service.impl;

import com.giasuhq.dto.request.CreateTutorPostRequest;
import com.giasuhq.dto.request.ModeratePostRequest;
import com.giasuhq.dto.request.UpdateTutorPostRequest;
import com.giasuhq.dto.response.TutorPostResponse;
import com.giasuhq.entity.*;
import com.giasuhq.exception.ResourceNotFoundException;
import com.giasuhq.repository.SubjectRepository;
import com.giasuhq.repository.TutorApplicationRepository;
import com.giasuhq.repository.TutorPostRepository;
import com.giasuhq.service.TutorPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TutorPostServiceImpl implements TutorPostService {

    private final TutorPostRepository tutorPostRepository;
    private final TutorApplicationRepository tutorApplicationRepository;
    private final SubjectRepository subjectRepository;

    private static final Pattern PHONE_REGEX = Pattern.compile("(?:\\+?84|0)(?:\\s*\\d){9,10}");

    @Override
    @Transactional
    public TutorPostResponse createPost(CreateTutorPostRequest request, User currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập để tạo bài đăng tìm gia sư.");
        }

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học với ID: " + request.getSubjectId()));

        TutorPost post = TutorPost.builder()
                .creator(currentUser)
                .title(request.getTitle().trim())
                .subject(subject)
                .gradeLevel(request.getGradeLevel().trim())
                .teachingMode(request.getTeachingMode())
                .city(request.getCity() != null && !request.getCity().trim().isEmpty() ? request.getCity().trim() : "Hà Nội")
                .district(request.getDistrict() != null ? request.getDistrict().trim() : null)
                .detailedAddress(request.getDetailedAddress() != null ? request.getDetailedAddress().trim() : null)
                .sessionsPerWeek(request.getSessionsPerWeek() != null && request.getSessionsPerWeek() > 0 ? request.getSessionsPerWeek() : 2)
                .preferredSchedule(request.getPreferredSchedule() != null ? request.getPreferredSchedule().trim() : null)
                .budgetAmount(request.getBudgetAmount() != null && request.getBudgetAmount().compareTo(BigDecimal.ZERO) > 0 ? request.getBudgetAmount() : null)
                .budgetType(request.getBudgetType() != null ? request.getBudgetType() : BudgetType.PER_SESSION)
                .tutorGenderPreference(request.getTutorGenderPreference() != null ? request.getTutorGenderPreference() : TutorGenderPreference.ANY)
                .tutorTypePreference(request.getTutorTypePreference() != null ? request.getTutorTypePreference() : TutorTypePreference.ANY)
                .studentNotes(request.getStudentNotes() != null ? request.getStudentNotes().trim() : null)
                .requirements(request.getRequirements() != null ? request.getRequirements().trim() : null)
                .status(TutorPostStatus.PENDING_APPROVAL)
                .build();

        TutorPost savedPost = tutorPostRepository.save(post);
        log.info("Created new tutor post: id={}, creator={}", savedPost.getId(), currentUser.getEmail());
        return mapToResponse(savedPost, currentUser, true);
    }

    @Override
    @Transactional
    public TutorPostResponse updatePost(Long id, UpdateTutorPostRequest request, User currentUser) {
        TutorPost post = tutorPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài đăng tìm gia sư với ID: " + id));

        boolean isCreator = post.getCreator().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isCreator && !isAdmin) {
            throw new IllegalArgumentException("Bạn không có quyền chỉnh sửa bài đăng này.");
        }

        if (post.getStatus() == TutorPostStatus.MATCHED || post.getStatus() == TutorPostStatus.CLOSED) {
            throw new IllegalArgumentException("Không thể chỉnh sửa bài đăng đã đóng hoặc đã chọn gia sư.");
        }

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học với ID: " + request.getSubjectId()));

        post.setTitle(request.getTitle().trim());
        post.setSubject(subject);
        post.setGradeLevel(request.getGradeLevel().trim());
        post.setTeachingMode(request.getTeachingMode());
        post.setCity(request.getCity() != null ? request.getCity().trim() : null);
        post.setDistrict(request.getDistrict() != null ? request.getDistrict().trim() : null);
        post.setDetailedAddress(request.getDetailedAddress() != null ? request.getDetailedAddress().trim() : null);
        post.setSessionsPerWeek(request.getSessionsPerWeek() != null && request.getSessionsPerWeek() > 0 ? request.getSessionsPerWeek() : 2);
        post.setPreferredSchedule(request.getPreferredSchedule() != null ? request.getPreferredSchedule().trim() : null);
        post.setBudgetAmount(request.getBudgetAmount());
        post.setBudgetType(request.getBudgetType() != null ? request.getBudgetType() : BudgetType.PER_SESSION);
        post.setTutorGenderPreference(request.getTutorGenderPreference() != null ? request.getTutorGenderPreference() : TutorGenderPreference.ANY);
        post.setTutorTypePreference(request.getTutorTypePreference() != null ? request.getTutorTypePreference() : TutorTypePreference.ANY);
        post.setStudentNotes(request.getStudentNotes() != null ? request.getStudentNotes().trim() : null);
        post.setRequirements(request.getRequirements() != null ? request.getRequirements().trim() : null);

        // If post was rejected previously, editing re-submits it for approval
        if (post.getStatus() == TutorPostStatus.REJECTED && isCreator) {
            post.setStatus(TutorPostStatus.PENDING_APPROVAL);
            post.setRejectionReason(null);
        }

        TutorPost updated = tutorPostRepository.save(post);
        return mapToResponse(updated, currentUser, true);
    }

    @Override
    @Transactional(readOnly = true)
    public TutorPostResponse getPostById(Long id, User currentUser) {
        TutorPost post = tutorPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài đăng tìm gia sư với ID: " + id));

        boolean isCreator = currentUser != null && post.getCreator().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser != null && currentUser.getRole() == Role.ADMIN;
        boolean isAcceptedTutor = false;

        if (currentUser != null && currentUser.getRole() == Role.TUTOR) {
            Optional<TutorApplication> app = tutorApplicationRepository.findByPostIdAndTutorId(post.getId(), currentUser.getId());
            if (app.isPresent() && app.get().getStatus() == TutorApplicationStatus.ACCEPTED) {
                isAcceptedTutor = true;
            }
        }

        boolean canSeeSensitive = isCreator || isAdmin || isAcceptedTutor;
        return mapToResponse(post, currentUser, canSeeSensitive);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TutorPostResponse> getMyPosts(User currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập để xem danh sách bài đăng của bạn.");
        }
        List<TutorPost> posts = tutorPostRepository.findByCreatorIdOrderByCreatedAtDesc(currentUser.getId());
        return posts.stream()
                .map(p -> mapToResponse(p, currentUser, true))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TutorPostResponse> searchPublicPosts(
            Long subjectId,
            String gradeLevel,
            TeachingMode teachingMode,
            String city,
            String district,
            BigDecimal minBudget,
            BigDecimal maxBudget,
            String keyword,
            User currentUser,
            Pageable pageable
    ) {
        Page<TutorPost> page = tutorPostRepository.searchPublicPosts(
                TutorPostStatus.OPEN,
                subjectId,
                gradeLevel,
                teachingMode,
                city,
                district,
                minBudget,
                maxBudget,
                keyword,
                pageable
        );

        return page.map(p -> {
            boolean isCreator = currentUser != null && p.getCreator().getId().equals(currentUser.getId());
            boolean isAdmin = currentUser != null && currentUser.getRole() == Role.ADMIN;
            return mapToResponse(p, currentUser, isCreator || isAdmin);
        });
    }

    @Override
    @Transactional
    public TutorPostResponse closePost(Long id, User currentUser) {
        TutorPost post = tutorPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài đăng tìm gia sư với ID: " + id));

        boolean isCreator = currentUser != null && post.getCreator().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser != null && currentUser.getRole() == Role.ADMIN;
        if (!isCreator && !isAdmin) {
            throw new IllegalArgumentException("Bạn không có quyền đóng bài đăng này.");
        }

        post.setStatus(TutorPostStatus.CLOSED);
        TutorPost updated = tutorPostRepository.save(post);
        return mapToResponse(updated, currentUser, true);
    }

    @Override
    @Transactional
    public TutorPostResponse moderatePost(Long id, ModeratePostRequest request, User adminUser) {
        if (adminUser == null || adminUser.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Chỉ Quản trị viên (Admin) mới có quyền kiểm duyệt bài đăng.");
        }

        TutorPost post = tutorPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài đăng tìm gia sư với ID: " + id));

        String action = request.getAction() != null ? request.getAction().trim().toUpperCase() : "";
        if ("APPROVE".equals(action)) {
            post.setStatus(TutorPostStatus.OPEN);
            post.setRejectionReason(null);
        } else if ("REJECT".equals(action)) {
            post.setStatus(TutorPostStatus.REJECTED);
            post.setRejectionReason(request.getRejectionReason() != null ? request.getRejectionReason().trim() : "Nội dung bài đăng chưa đáp ứng tiêu chuẩn cộng đồng.");
        } else {
            throw new IllegalArgumentException("Hành động kiểm duyệt không hợp lệ. Chỉ chấp nhận APPROVE hoặc REJECT.");
        }

        TutorPost updated = tutorPostRepository.save(post);
        log.info("Admin {} moderated post {}: status={}", adminUser.getEmail(), post.getId(), post.getStatus());
        return mapToResponse(updated, adminUser, true);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TutorPostResponse> getAllPostsForAdmin(Pageable pageable) {
        return tutorPostRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(p -> mapToResponse(p, null, true));
    }

    private TutorPostResponse mapToResponse(TutorPost post, User currentUser, boolean canSeeSensitive) {
        long applicationsCount = tutorApplicationRepository.countByPostId(post.getId());

        boolean hasApplied = false;
        Long myApplicationId = null;

        if (currentUser != null && currentUser.getRole() == Role.TUTOR) {
            Optional<TutorApplication> app = tutorApplicationRepository.findByPostIdAndTutorId(post.getId(), currentUser.getId());
            if (app.isPresent()) {
                hasApplied = true;
                myApplicationId = app.get().getId();
            }
        }

        String displayedAddress = canSeeSensitive ? post.getDetailedAddress() : null;
        String displayedPhone = canSeeSensitive ? post.getCreator().getPhone() : null;
        String displayedEmail = canSeeSensitive ? post.getCreator().getEmail() : null;

        String studentNotes = post.getStudentNotes();
        String requirements = post.getRequirements();

        if (!canSeeSensitive) {
            studentNotes = maskSensitiveText(studentNotes);
            requirements = maskSensitiveText(requirements);
        }

        return TutorPostResponse.builder()
                .id(post.getId())
                .creatorId(post.getCreator().getId())
                .creatorName(post.getCreator().getFullName())
                .creatorAvatar(post.getCreator().getAvatarUrl())
                .creatorPhone(displayedPhone)
                .creatorEmail(displayedEmail)
                .title(post.getTitle())
                .subjectId(post.getSubject().getId())
                .subjectName(post.getSubject().getName())
                .subjectCode(post.getSubject().getCode())
                .gradeLevel(post.getGradeLevel())
                .teachingMode(post.getTeachingMode())
                .city(post.getCity())
                .district(post.getDistrict())
                .detailedAddress(displayedAddress)
                .sessionsPerWeek(post.getSessionsPerWeek())
                .preferredSchedule(post.getPreferredSchedule())
                .budgetAmount(post.getBudgetAmount())
                .budgetType(post.getBudgetType())
                .tutorGenderPreference(post.getTutorGenderPreference())
                .tutorTypePreference(post.getTutorTypePreference())
                .studentNotes(studentNotes)
                .requirements(requirements)
                .status(post.getStatus())
                .rejectionReason(post.getRejectionReason())
                .applicationsCount(applicationsCount)
                .hasApplied(hasApplied)
                .myApplicationId(myApplicationId)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    private String maskSensitiveText(String text) {
        if (text == null) return null;
        return PHONE_REGEX.matcher(text).replaceAll("[Số điện thoại đã ẩn]");
    }
}
