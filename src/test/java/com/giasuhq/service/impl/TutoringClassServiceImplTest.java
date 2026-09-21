package com.giasuhq.service.impl;

import com.giasuhq.dto.request.CreateClassRequest;
import com.giasuhq.entity.*;
import com.giasuhq.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TutoringClassServiceImplTest {

    @Mock private TutoringClassRepository tutoringClassRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private UserRepository userRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private ParentRepository parentRepository;
    @Mock private LessonRepository lessonRepository;

    @InjectMocks
    private TutoringClassServiceImpl tutoringClassService;

    @Test
    void shouldOnlyExposeTutorAndParentRoles() {
        List<Role> roles = Arrays.asList(Role.values());
        assertEquals(2, roles.size());
        assertTrue(roles.contains(Role.TUTOR));
        assertTrue(roles.contains(Role.PARENT));
    }

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

        assertThrows(IllegalArgumentException.class, () -> tutoringClassService.createClass(request, currentUser));
    }
}
