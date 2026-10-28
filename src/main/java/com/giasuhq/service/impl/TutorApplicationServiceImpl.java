package com.giasuhq.service.impl;

import com.giasuhq.dto.request.ApplyTutorPostRequest;
import com.giasuhq.dto.request.RejectApplicationRequest;
import com.giasuhq.dto.response.TutorApplicationResponse;
import com.giasuhq.entity.*;
import com.giasuhq.exception.ResourceNotFoundException;
import com.giasuhq.repository.*;
import com.giasuhq.service.TutorApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TutorApplicationServiceImpl implements TutorApplicationService {

    private final TutorApplicationRepository tutorApplicationRepository;
    private final TutorPostRepository tutorPostRepository;
    private final TutorRepository tutorRepository;
    private final ParentRepository parentRepository;
    private final TutoringClassRepository tutoringClassRepository;
    private final ReviewRepository reviewRepository;

    @Override
    @Transactional
    public TutorApplicationResponse apply(Long postId, ApplyTutorPostRequest request, User currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập để nộp đơn ứng tuyển.");
        }
        if (currentUser.getRole() != Role.TUTOR) {
            throw new IllegalArgumentException("Chỉ tài khoản Gia sư (TUTOR) mới có quyền nộp đơn ứng tuyển nhận lớp.");
        }

        TutorPost post = tutorPostRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài đăng tìm gia sư với ID: " + postId));

        if (post.getStatus() != TutorPostStatus.OPEN) {
            throw new IllegalArgumentException("Bài đăng hiện không mở nhận đơn ứng tuyển (trạng thái: " + post.getStatus() + ").");
        }

        if (post.getCreator().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Bạn không thể tự ứng tuyển vào bài đăng do chính mình tạo.");
        }

        if (tutorApplicationRepository.existsByPostIdAndTutorId(postId, currentUser.getId())) {
            throw new IllegalArgumentException("Bạn đã nộp đơn ứng tuyển cho bài đăng này rồi.");
        }

        Tutor tutor = tutorRepository.findById(currentUser.getId()).orElseGet(() ->
                tutorRepository.save(Tutor.builder()
                        .id(currentUser.getId())
                        .email(currentUser.getEmail())
                        .fullName(currentUser.getFullName())
                        .phone(currentUser.getPhone())
                        .role(Role.TUTOR)
                        .verificationStatus("PENDING")
                        .build())
        );

        TutorApplication application = TutorApplication.builder()
                .post(post)
                .tutor(tutor)
                .coverLetter(request.getCoverLetter().trim())
                .proposedRate(request.getProposedRate() != null && request.getProposedRate().compareTo(BigDecimal.ZERO) > 0 ? request.getProposedRate() : post.getBudgetAmount())
                .status(TutorApplicationStatus.PENDING)
                .build();

        TutorApplication savedApp = tutorApplicationRepository.save(application);
        log.info("Tutor {} applied to post {}: app_id={}", currentUser.getEmail(), post.getId(), savedApp.getId());
        return mapToResponse(savedApp, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TutorApplicationResponse> getApplicationsForPost(Long postId, User currentUser) {
        TutorPost post = tutorPostRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài đăng tìm gia sư với ID: " + postId));

        boolean isCreator = currentUser != null && post.getCreator().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser != null && currentUser.getRole() == Role.ADMIN;
        if (!isCreator && !isAdmin) {
            throw new IllegalArgumentException("Bạn không có quyền xem danh sách ứng viên của bài đăng này.");
        }

        List<TutorApplication> apps = tutorApplicationRepository.findByPostIdOrderByCreatedAtDesc(postId);
        return apps.stream()
                .map(app -> mapToResponse(app, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TutorApplicationResponse> getMyApplications(User currentUser) {
        if (currentUser == null || currentUser.getRole() != Role.TUTOR) {
            throw new IllegalArgumentException("Vui lòng đăng nhập với tư cách Gia sư để xem danh sách đơn ứng tuyển.");
        }

        List<TutorApplication> apps = tutorApplicationRepository.findByTutorIdOrderByCreatedAtDesc(currentUser.getId());
        return apps.stream()
                .map(app -> mapToResponse(app, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TutorApplicationResponse acceptApplication(Long postId, Long applicationId, User currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập.");
        }

        TutorPost post = tutorPostRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài đăng tìm gia sư với ID: " + postId));

        boolean isCreator = post.getCreator().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isCreator && !isAdmin) {
            throw new IllegalArgumentException("Bạn không có quyền chọn gia sư cho bài đăng này.");
        }

        if (post.getStatus() != TutorPostStatus.OPEN) {
            throw new IllegalArgumentException("Bài đăng hiện không ở trạng thái mở nhận gia sư.");
        }

        TutorApplication application = tutorApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn ứng tuyển với ID: " + applicationId));

        if (!application.getPost().getId().equals(postId)) {
            throw new IllegalArgumentException("Đơn ứng tuyển không thuộc bài đăng này.");
        }

        if (application.getStatus() != TutorApplicationStatus.PENDING) {
            throw new IllegalArgumentException("Đơn ứng tuyển này không ở trạng thái chờ duyệt (trạng thái: " + application.getStatus() + ").");
        }

        // 1. Accept this application
        application.setStatus(TutorApplicationStatus.ACCEPTED);
        application.setResolvedAt(LocalDateTime.now());
        tutorApplicationRepository.save(application);

        // 2. Reject all other pending applications for this post
        List<TutorApplication> otherPending = tutorApplicationRepository.findByPostIdAndStatus(postId, TutorApplicationStatus.PENDING);
        for (TutorApplication other : otherPending) {
            if (!other.getId().equals(applicationId)) {
                other.setStatus(TutorApplicationStatus.REJECTED);
                other.setRejectionNote("Phụ huynh đã chọn gia sư khác cho lớp học này.");
                other.setResolvedAt(LocalDateTime.now());
            }
        }
        if (!otherPending.isEmpty()) {
            tutorApplicationRepository.saveAll(otherPending);
        }

        // 3. Mark post as MATCHED
        post.setStatus(TutorPostStatus.MATCHED);
        tutorPostRepository.save(post);

        // 4. Automatically create TutoringClass
        User creator = post.getCreator();
        Parent parent = parentRepository.findById(creator.getId()).orElseGet(() ->
                parentRepository.save(Parent.builder()
                        .id(creator.getId())
                        .email(creator.getEmail())
                        .fullName(creator.getFullName())
                        .phone(creator.getPhone())
                        .role(Role.PARENT)
                        .address(post.getDetailedAddress())
                        .studentName(creator.getFullName())
                        .studentGradeLevel(post.getGradeLevel())
                        .build())
        );

        String scheduleDesc = post.getPreferredSchedule() != null && !post.getPreferredSchedule().isBlank()
                ? post.getPreferredSchedule()
                : ("Lịch học linh hoạt theo thỏa thuận (" + post.getSessionsPerWeek() + " buổi/tuần)");

        BigDecimal connectionFee = new BigDecimal("5000");

        TutoringClass tutoringClass = TutoringClass.builder()
                .className(post.getTitle())
                .tutor(application.getTutor())
                .parent(parent)
                .subject(post.getSubject())
                .studentName(parent.getStudentName() != null ? parent.getStudentName() : creator.getFullName())
                .studentGradeLevel(post.getGradeLevel())
                .scheduleDescription(scheduleDesc)
                .connectionFee(connectionFee)
                .status(ClassStatus.PENDING_PAYMENT)
                .build();

        TutoringClass savedClass = tutoringClassRepository.save(tutoringClass);
        log.info("Created TutoringClass {} from accepted tutor post {} and app {}", savedClass.getId(), post.getId(), application.getId());

        return mapToResponse(application, savedClass.getId());
    }

    @Override
    @Transactional
    public TutorApplicationResponse rejectApplication(Long postId, Long applicationId, RejectApplicationRequest request, User currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập.");
        }

        TutorPost post = tutorPostRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài đăng tìm gia sư với ID: " + postId));

        boolean isCreator = post.getCreator().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isCreator && !isAdmin) {
            throw new IllegalArgumentException("Bạn không có quyền từ chối ứng viên cho bài đăng này.");
        }

        TutorApplication application = tutorApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn ứng tuyển với ID: " + applicationId));

        if (!application.getPost().getId().equals(postId)) {
            throw new IllegalArgumentException("Đơn ứng tuyển không thuộc bài đăng này.");
        }

        application.setStatus(TutorApplicationStatus.REJECTED);
        application.setRejectionNote(request != null && request.getRejectionNote() != null ? request.getRejectionNote().trim() : "Hồ sơ chưa phù hợp với yêu cầu hiện tại.");
        application.setResolvedAt(LocalDateTime.now());

        TutorApplication updated = tutorApplicationRepository.save(application);
        return mapToResponse(updated, null);
    }

    @Override
    @Transactional
    public TutorApplicationResponse withdrawApplication(Long applicationId, User currentUser) {
        if (currentUser == null || currentUser.getRole() != Role.TUTOR) {
            throw new IllegalArgumentException("Vui lòng đăng nhập với tư cách Gia sư.");
        }

        TutorApplication application = tutorApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn ứng tuyển với ID: " + applicationId));

        if (!application.getTutor().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Bạn không thể rút đơn ứng tuyển của người khác.");
        }

        if (application.getStatus() != TutorApplicationStatus.PENDING) {
            throw new IllegalArgumentException("Chỉ có thể rút đơn ứng tuyển khi đang ở trạng thái chờ duyệt (PENDING).");
        }

        application.setStatus(TutorApplicationStatus.WITHDRAWN);
        application.setResolvedAt(LocalDateTime.now());

        TutorApplication updated = tutorApplicationRepository.save(application);
        log.info("Tutor {} withdrew application {}", currentUser.getEmail(), applicationId);
        return mapToResponse(updated, null);
    }

    private TutorApplicationResponse mapToResponse(TutorApplication app, Long matchedClassId) {
        Tutor tutor = app.getTutor();
        Double avgRating = reviewRepository.getAverageRatingByTutorId(tutor.getId());

        boolean isResolvedAccepted = app.getStatus() == TutorApplicationStatus.ACCEPTED;

        return TutorApplicationResponse.builder()
                .id(app.getId())
                .postId(app.getPost().getId())
                .postTitle(app.getPost().getTitle())
                .tutorId(tutor.getId())
                .tutorName(tutor.getFullName())
                .tutorEmail(isResolvedAccepted ? tutor.getEmail() : null)
                .tutorPhone(isResolvedAccepted ? tutor.getPhone() : null)
                .tutorAvatar(tutor.getAvatarUrl())
                .tutorBio(tutor.getBio())
                .tutorQualification(tutor.getQualification())
                .tutorExperienceYears(tutor.getExperienceYears())
                .tutorRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 5.0)
                .coverLetter(app.getCoverLetter())
                .proposedRate(app.getProposedRate())
                .status(app.getStatus())
                .rejectionNote(app.getRejectionNote())
                .createdAt(app.getCreatedAt())
                .resolvedAt(app.getResolvedAt())
                .tutoringClassId(matchedClassId)
                .build();
    }
}
