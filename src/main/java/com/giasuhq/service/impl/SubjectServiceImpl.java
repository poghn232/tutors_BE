package com.giasuhq.service.impl;

import com.giasuhq.dto.request.CreateSubjectRequest;
import com.giasuhq.dto.response.SubjectResponse;
import com.giasuhq.entity.Subject;
import com.giasuhq.repository.SubjectRepository;
import com.giasuhq.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SubjectResponse> getAllSubjects() {
        seedDefaultSubjectsIfEmpty();
        return subjectRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SubjectResponse createSubject(CreateSubjectRequest request) {
        if (subjectRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Mã môn học '" + request.getCode() + "' đã tồn tại.");
        }

        Subject subject = Subject.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .build();

        Subject saved = subjectRepository.save(subject);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void seedDefaultSubjectsIfEmpty() {
        if (subjectRepository.count() == 0) {
            List<Subject> defaults = Arrays.asList(
                    Subject.builder().code("MATH").name("Toán học").description("Chương trình Toán THCS & THPT, ôn thi ĐHQG / THPT QG").build(),
                    Subject.builder().code("PHYS").name("Vật lý").description("Chương trình Vật lý Phổ thông & Luyện thi Đại học").build(),
                    Subject.builder().code("CHEM").name("Hóa học").description("Hóa học Đại số, Hữu cơ & Vô cơ các khối").build(),
                    Subject.builder().code("ENG").name("Tiếng Anh").description("Tiếng Anh Phổ thông, IELTS, TOEIC, Giao tiếp").build(),
                    Subject.builder().code("LIT").name("Ngữ văn").description("Ngữ văn & Đọc hiểu tác phẩm, Luyện viết văn phân tích").build(),
                    Subject.builder().code("BIO").name("Sinh học").description("Sinh học THPT & Ôn thi các khối B").build(),
                    Subject.builder().code("INF").name("Tin học / Lập trình").description("Tin học ứng dụng, Lập trình Python, C++, Web").build()
            );
            subjectRepository.saveAll(defaults);
        }
    }

    private SubjectResponse mapToResponse(Subject subject) {
        return SubjectResponse.builder()
                .id(subject.getId())
                .code(subject.getCode())
                .name(subject.getName())
                .description(subject.getDescription())
                .build();
    }
}
