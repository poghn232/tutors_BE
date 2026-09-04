package com.giasuhq.service;

import com.giasuhq.dto.request.CreateSubjectRequest;
import com.giasuhq.dto.response.SubjectResponse;
import java.util.List;

public interface SubjectService {
    List<SubjectResponse> getAllSubjects();
    SubjectResponse createSubject(CreateSubjectRequest request);
    void seedDefaultSubjectsIfEmpty();
}
