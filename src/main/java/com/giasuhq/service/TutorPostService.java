package com.giasuhq.service;

import com.giasuhq.dto.request.CreateTutorPostRequest;
import com.giasuhq.dto.request.ModeratePostRequest;
import com.giasuhq.dto.request.UpdateTutorPostRequest;
import com.giasuhq.dto.response.TutorPostResponse;
import com.giasuhq.entity.TeachingMode;
import com.giasuhq.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface TutorPostService {
    TutorPostResponse createPost(CreateTutorPostRequest request, User currentUser);
    TutorPostResponse updatePost(Long id, UpdateTutorPostRequest request, User currentUser);
    TutorPostResponse getPostById(Long id, User currentUser);
    List<TutorPostResponse> getMyPosts(User currentUser);
    Page<TutorPostResponse> searchPublicPosts(
            Long subjectId,
            String gradeLevel,
            TeachingMode teachingMode,
            String city,
            String district,
            BigDecimal minBudget,
            BigDecimal maxBudget,
            String keyword,
            User currentUser,
            Pageable pageable
    );
    TutorPostResponse closePost(Long id, User currentUser);
    TutorPostResponse moderatePost(Long id, ModeratePostRequest request, User adminUser);
    Page<TutorPostResponse> getAllPostsForAdmin(Pageable pageable);
}
