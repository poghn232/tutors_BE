package com.giasuhq.service;

import com.giasuhq.dto.request.CreateClassRequest;
import com.giasuhq.dto.response.ClassResponse;
import com.giasuhq.entity.User;
import java.util.List;

public interface TutoringClassService {
    List<ClassResponse> getClassesForUser(User currentUser);
    ClassResponse getClassById(Long id, User currentUser);
    ClassResponse createClass(CreateClassRequest request, User currentUser);
}
