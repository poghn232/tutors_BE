package com.giasuhq.service;

import com.giasuhq.dto.request.CreateLessonNoteRequest;
import com.giasuhq.dto.request.CreateLessonRequest;
import com.giasuhq.dto.response.LessonNoteResponse;
import com.giasuhq.dto.response.LessonResponse;
import com.giasuhq.entity.LessonStatus;
import com.giasuhq.entity.User;

import java.util.List;

public interface LessonService {
    LessonResponse createLesson(CreateLessonRequest request, User currentUser);
    List<LessonResponse> getLessonsForUser(User currentUser);
    LessonResponse updateLessonStatus(Long lessonId, LessonStatus status, User currentUser);
    LessonNoteResponse addOrUpdateLessonNote(Long lessonId, CreateLessonNoteRequest request, User currentUser);
}
