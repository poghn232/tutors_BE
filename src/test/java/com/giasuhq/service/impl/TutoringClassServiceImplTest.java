package com.giasuhq.service.impl;

import com.giasuhq.dto.request.CreateClassRequest;
import com.giasuhq.entity.*;
import com.giasuhq.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TutoringClassServiceImplTest {

    @Mock private TutoringClassRepository tutoringClassRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private UserRepository userRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private ParentRepository parentRepository;
    @Mock private LessonRepository lessonRepository;

    @InjectMocks
    private TutoringClassServiceImpl tutoringClassService;

    @Test
    void shouldRejectMissingRequiredBookingInfo() {
        CreateClassRequest request = CreateClassRequest.builder()
                .className("Lớp Toán 12")
                .scheduleDescription("Thứ 2 - 4 (18:00)")
                .build();

        User currentUser = User.builder()
                .id(10L)
                .fullName("Phụ huynh A")
                .role(Role.PARENT)
                .build();

        when(subjectRepository.findAll()).thenReturn(List.of(Subject.builder().id(1L).code("MATH").name("Toán Học").build()));
        when(subjectRepository.save(any(Subject.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(tutorRepository.findAll()).thenReturn(List.of(Tutor.builder().id(1L).email("tutor@test.com").fullName("Gia sư A").role(Role.TUTOR).build()));
        when(tutorRepository.save(any(Tutor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(studentRepository.findAll()).thenReturn(List.of(Student.builder().id(2L).email("student@test.com").fullName("Học sinh A").role(Role.STUDENT).build()));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(parentRepository.save(any(Parent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(tutoringClassRepository.save(any(TutoringClass.class))).thenAnswer(invocation -> {
            TutoringClass entity = invocation.getArgument(0);
            entity.setId(99L);
            return entity;
        });

        when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> {
            Lesson entity = invocation.getArgument(0);
            entity.setId(77L);
            return entity;
        });

        assertThrows(IllegalArgumentException.class, () -> tutoringClassService.createClass(request, currentUser));
    }
}
