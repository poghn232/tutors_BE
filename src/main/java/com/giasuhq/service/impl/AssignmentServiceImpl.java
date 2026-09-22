package com.giasuhq.service.impl;

import com.giasuhq.dto.request.CreateAssignmentRequest;
import com.giasuhq.dto.request.GradeAssignmentRequest;
import com.giasuhq.dto.request.SubmitAssignmentRequest;
import com.giasuhq.dto.response.AssignmentResponse;
import com.giasuhq.dto.response.ParentSummaryResponse;
import com.giasuhq.entity.*;
import com.giasuhq.repository.AssignmentRepository;
import com.giasuhq.repository.ParentRepository;
import com.giasuhq.repository.TutoringClassRepository;
import com.giasuhq.repository.UserRepository;
import com.giasuhq.service.AssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final ParentRepository parentRepository;
    private final UserRepository userRepository;
    private final TutoringClassRepository tutoringClassRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ParentSummaryResponse> searchParents(String keyword) {
        List<Parent> parents;
        if (keyword == null || keyword.trim().isEmpty()) {
            parents = parentRepository.findAll();
        } else {
            parents = parentRepository.searchParents(keyword.trim());
        }

        return parents.stream().map(p -> ParentSummaryResponse.builder()
                .id(p.getId())
                .fullName(p.getFullName())
                .email(p.getEmail())
                .phone(p.getPhone())
                .studentName(p.getStudentName() != null ? p.getStudentName() : p.getFullName())
                .studentGradeLevel(p.getStudentGradeLevel())
                .studentSchoolName(p.getStudentSchoolName())
                .avatarUrl(p.getAvatarUrl())
                .build()
        ).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AssignmentResponse createAssignment(User tutor, CreateAssignmentRequest request) {
        if (tutor.getRole() != Role.TUTOR && tutor.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Chỉ gia sư mới có quyền tạo bài tập.");
        }

        User parent = userRepository.findById(request.getParentId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin phụ huynh nhận bài tập."));

        if (request.getDueDate() == null) {
            throw new IllegalArgumentException("Vui lòng chọn hạn nộp bài tập.");
        }

        TutoringClass tutoringClass = null;
        if (request.getClassId() != null) {
            tutoringClass = tutoringClassRepository.findById(request.getClassId()).orElse(null);
        }

        Assignment assignment = Assignment.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .subjectName(request.getSubjectName())
                .tutor(tutor)
                .parent(parent)
                .tutoringClass(tutoringClass)
                .attachmentUrl(request.getAttachmentUrl())
                .attachmentName(request.getAttachmentName())
                .attachmentSize(request.getAttachmentSize())
                .dueDate(request.getDueDate())
                .status(AssignmentStatus.PENDING)
                .build();

        Assignment saved = assignmentRepository.save(assignment);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public List<AssignmentResponse> getAssignmentsForUser(User user) {
        checkAndLockOverdueAssignments();

        List<Assignment> list;
        if (user.getRole() == Role.TUTOR) {
            list = assignmentRepository.findByTutorIdOrderByCreatedAtDesc(user.getId());
        } else if (user.getRole() == Role.PARENT) {
            list = assignmentRepository.findByParentIdOrderByCreatedAtDesc(user.getId());
        } else {
            list = assignmentRepository.findAll();
        }

        return list.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AssignmentResponse getAssignmentById(Long id, User user) {
        checkAndLockOverdueAssignments();

        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài tập với mã: " + id));

        // Authorization check
        if (user.getRole() != Role.ADMIN &&
            !assignment.getTutor().getId().equals(user.getId()) &&
            !assignment.getParent().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Bạn không có quyền truy cập bài tập này.");
        }

        return mapToResponse(assignment);
    }

    @Override
    @Transactional
    public AssignmentResponse submitAssignment(User parent, Long assignmentId, SubmitAssignmentRequest request) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài tập với mã: " + assignmentId));

        if (!assignment.getParent().getId().equals(parent.getId()) && parent.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Bạn không có quyền nộp bài tập của người khác.");
        }

        // Check if deadline passed
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(assignment.getDueDate()) || assignment.getStatus() == AssignmentStatus.NOT_SUBMITTED) {
            assignment.setStatus(AssignmentStatus.NOT_SUBMITTED);
            assignmentRepository.save(assignment);
            throw new IllegalArgumentException("Bài tập đã quá hạn nộp. Hệ thống đã khóa và không cho phép nộp bù!");
        }

        assignment.setSubmittedFileUrl(request.getSubmittedFileUrl());
        assignment.setSubmittedFileName(request.getSubmittedFileName());
        assignment.setSubmittedFileSize(request.getSubmittedFileSize());
        assignment.setSubmissionNote(request.getSubmissionNote());
        assignment.setSubmittedAt(now);
        assignment.setStatus(AssignmentStatus.SUBMITTED);

        Assignment saved = assignmentRepository.save(assignment);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public AssignmentResponse gradeAssignment(User tutor, Long assignmentId, GradeAssignmentRequest request) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài tập với mã: " + assignmentId));

        if (!assignment.getTutor().getId().equals(tutor.getId()) && tutor.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Bạn không có quyền chấm bài tập này.");
        }

        if (assignment.getStatus() != AssignmentStatus.SUBMITTED && assignment.getStatus() != AssignmentStatus.GRADED) {
            throw new IllegalArgumentException("Chỉ có thể chấm bài tập đã được học sinh/phụ huynh nộp.");
        }

        BigDecimal rating = request.getRating();
        if (rating == null || rating.compareTo(BigDecimal.ZERO) < 0 || rating.compareTo(BigDecimal.valueOf(10.0)) > 0) {
            throw new IllegalArgumentException("Điểm đánh giá phải nằm trong khoảng từ 0.0 đến 10.0");
        }

        assignment.setRating(rating);
        assignment.setTutorComment(request.getTutorComment());
        assignment.setGradedAt(LocalDateTime.now());
        assignment.setStatus(AssignmentStatus.GRADED);

        Assignment saved = assignmentRepository.save(assignment);
        return mapToResponse(saved);
    }

    private void checkAndLockOverdueAssignments() {
        LocalDateTime now = LocalDateTime.now();
        List<Assignment> overduePending = assignmentRepository.findOverduePendingAssignments(AssignmentStatus.PENDING, now);
        if (!overduePending.isEmpty()) {
            for (Assignment a : overduePending) {
                a.setStatus(AssignmentStatus.NOT_SUBMITTED);
            }
            assignmentRepository.saveAll(overduePending);
        }
    }

    private AssignmentResponse mapToResponse(Assignment a) {
        boolean isOverdue = LocalDateTime.now().isAfter(a.getDueDate());
        AssignmentStatus status = a.getStatus();
        if (status == AssignmentStatus.PENDING && isOverdue) {
            status = AssignmentStatus.NOT_SUBMITTED;
        }

        String statusLabel;
        switch (status) {
            case SUBMITTED:
                statusLabel = "Đã nộp";
                break;
            case GRADED:
                statusLabel = "Đã chấm";
                break;
            case NOT_SUBMITTED:
                statusLabel = "Không nộp (Quá hạn)";
                break;
            case PENDING:
            default:
                statusLabel = "Đang mở (Chờ nộp)";
                break;
        }

        String studentName = a.getParent().getFullName();
        if (a.getParent() instanceof Parent) {
            Parent p = (Parent) a.getParent();
            if (p.getStudentName() != null && !p.getStudentName().isBlank()) {
                studentName = p.getStudentName();
            }
        }

        return AssignmentResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .description(a.getDescription())
                .subjectName(a.getSubjectName())
                .tutorId(a.getTutor().getId())
                .tutorName(a.getTutor().getFullName())
                .tutorEmail(a.getTutor().getEmail())
                .parentId(a.getParent().getId())
                .parentName(a.getParent().getFullName())
                .parentEmail(a.getParent().getEmail())
                .studentName(studentName)
                .classId(a.getTutoringClass() != null ? a.getTutoringClass().getId() : null)
                .className(a.getTutoringClass() != null ? a.getTutoringClass().getClassName() : null)
                .attachmentUrl(a.getAttachmentUrl())
                .attachmentName(a.getAttachmentName())
                .attachmentSize(a.getAttachmentSize())
                .dueDate(a.getDueDate())
                .status(status)
                .statusLabel(statusLabel)
                .submittedFileUrl(a.getSubmittedFileUrl())
                .submittedFileName(a.getSubmittedFileName())
                .submittedFileSize(a.getSubmittedFileSize())
                .submittedAt(a.getSubmittedAt())
                .submissionNote(a.getSubmissionNote())
                .rating(a.getRating())
                .tutorComment(a.getTutorComment())
                .gradedAt(a.getGradedAt())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .isOverdue(isOverdue)
                .build();
    }
}
