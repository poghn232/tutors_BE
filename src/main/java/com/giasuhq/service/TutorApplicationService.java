package com.giasuhq.service;

import com.giasuhq.dto.request.ApplyTutorPostRequest;
import com.giasuhq.dto.request.RejectApplicationRequest;
import com.giasuhq.dto.response.TutorApplicationResponse;
import com.giasuhq.entity.User;

import java.util.List;

public interface TutorApplicationService {
    TutorApplicationResponse apply(Long postId, ApplyTutorPostRequest request, User currentUser);
    List<TutorApplicationResponse> getApplicationsForPost(Long postId, User currentUser);
    List<TutorApplicationResponse> getMyApplications(User currentUser);
    TutorApplicationResponse acceptApplication(Long postId, Long applicationId, User currentUser);
    TutorApplicationResponse rejectApplication(Long postId, Long applicationId, RejectApplicationRequest request, User currentUser);
    TutorApplicationResponse withdrawApplication(Long applicationId, User currentUser);
}
