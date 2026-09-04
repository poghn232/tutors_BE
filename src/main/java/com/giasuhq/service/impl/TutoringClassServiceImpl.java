package com.giasuhq.service.impl;

import com.giasuhq.dto.request.CreateClassRequest;
import com.giasuhq.dto.response.ClassResponse;
import com.giasuhq.entity.*;
import com.giasuhq.exception.ResourceNotFoundException;
import com.giasuhq.repository.*;
import com.giasuhq.service.TutoringClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TutoringClassServiceImpl implements TutoringClassService {

    private final TutoringClassRepository tutoringClassRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final TutorRepository tutorRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ClassResponse> getClassesForUser(User currentUser) {
        List<TutoringClass> classes;
        Role role = currentUser.getRole();

        if (role == Role.TUTOR) {
            classes = tutoringClassRepository.findByTutorId(currentUser.getId());
        } else if (role == Role.PARENT) {
            classes = tutoringClassRepository.findByParentId(currentUser.getId());
        } else if (role == Role.STUDENT) {
            classes = tutoringClassRepository.findByStudentId(currentUser.getId());
        } else {
            classes = tutoringClassRepository.findAll();
        }

        return classes.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ClassResponse getClassById(Long id, User currentUser) {
        TutoringClass tutoringClass = tutoringClassRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + id));
        return mapToResponse(tutoringClass);
    }

    @Override
    @Transactional
    public ClassResponse createClass(CreateClassRequest request, User currentUser) {
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học với ID: " + request.getSubjectId()));

        Tutor tutor;
        if (currentUser instanceof Tutor) {
            tutor = (Tutor) currentUser;
        } else if (request.getTutorId() != null) {
            tutor = tutorRepository.findById(request.getTutorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gia sư với ID: " + request.getTutorId()));
        } else {
            // Lấy gia sư ngẫu nhiên/đầu tiên trong DB hoặc tạo mặc định
            tutor = tutorRepository.findAll().stream().findFirst().orElseGet(() -> 
                tutorRepository.save(Tutor.builder()
                        .email("tutor.default@giasuhq.com")
                        .fullName("Gia sư Mẫu")
                        .password("123456")
                        .role(Role.TUTOR)
                        .build())
            );
        }

        Student student = null;
        if (request.getStudentId() != null) {
            User userStudent = userRepository.findById(request.getStudentId()).orElse(null);
            if (userStudent instanceof Student) {
                student = (Student) userStudent;
            }
        }
        if (student == null) {
            String sName = request.getStudentName() != null && !request.getStudentName().isBlank() 
                    ? request.getStudentName() : "Học sinh Mới";
            String sEmail = request.getStudentEmail() != null && !request.getStudentEmail().isBlank()
                    ? request.getStudentEmail() : "student." + System.currentTimeMillis() + "@giasuhq.com";
            
            student = userRepository.save(Student.builder()
                    .email(sEmail)
                    .fullName(sName)
                    .password("123456")
                    .role(Role.STUDENT)
                    .gradeLevel("Lớp 12")
                    .build());
        }

        Parent parent = null;
        if (request.getParentId() != null) {
            User userParent = userRepository.findById(request.getParentId()).orElse(null);
            if (userParent instanceof Parent) {
                parent = (Parent) userParent;
            }
        }
        if (parent == null && currentUser instanceof Parent) {
            parent = (Parent) currentUser;
        }

        TutoringClass newClass = TutoringClass.builder()
                .className(request.getClassName())
                .subject(subject)
                .tutor(tutor)
                .student(student)
                .parent(parent)
                .scheduleDescription(request.getScheduleDescription() != null ? request.getScheduleDescription() : "Thứ 2 & 4 (18:00 - 20:00)")
                .status(ClassStatus.ACTIVE)
                .build();

        TutoringClass saved = tutoringClassRepository.save(newClass);
        return mapToResponse(saved);
    }

    private ClassResponse mapToResponse(TutoringClass tc) {
        return ClassResponse.builder()
                .id(tc.getId())
                .className(tc.getClassName())
                .tutorId(tc.getTutor() != null ? tc.getTutor().getId() : null)
                .tutorName(tc.getTutor() != null ? tc.getTutor().getFullName() : "Gia sư")
                .studentId(tc.getStudent() != null ? tc.getStudent().getId() : null)
                .studentName(tc.getStudent() != null ? tc.getStudent().getFullName() : "Học sinh")
                .parentId(tc.getParent() != null ? tc.getParent().getId() : null)
                .parentName(tc.getParent() != null ? tc.getParent().getFullName() : "Chưa gắn Phụ huynh")
                .subjectId(tc.getSubject() != null ? tc.getSubject().getId() : null)
                .subjectName(tc.getSubject() != null ? tc.getSubject().getName() : "Môn học")
                .scheduleDescription(tc.getScheduleDescription())
                .status(tc.getStatus())
                .createdAt(tc.getCreatedAt())
                .build();
    }
}
