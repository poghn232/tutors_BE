package com.giasuhq.service;

import com.giasuhq.dto.request.CreateAssignmentRequest;
import com.giasuhq.dto.request.GradeAssignmentRequest;
import com.giasuhq.dto.request.SubmitAssignmentRequest;
import com.giasuhq.dto.response.AssignmentResponse;
import com.giasuhq.dto.response.ParentSummaryResponse;
import com.giasuhq.entity.User;

import java.util.List;

public interface AssignmentService {

    List<ParentSummaryResponse> searchParents(String keyword);

    AssignmentResponse createAssignment(User tutor, CreateAssignmentRequest request);

    List<AssignmentResponse> getAssignmentsForUser(User user);

    AssignmentResponse getAssignmentById(Long id, User user);

    AssignmentResponse submitAssignment(User parent, Long assignmentId, SubmitAssignmentRequest request);

    AssignmentResponse gradeAssignment(User tutor, Long assignmentId, GradeAssignmentRequest request);
}
