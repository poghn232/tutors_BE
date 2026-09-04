package com.giasuhq.service;

import com.giasuhq.dto.request.UpdateProfileRequest;
import com.giasuhq.dto.response.UserProfileResponse;
import com.giasuhq.entity.User;

public interface UserService {
    UserProfileResponse getUserProfile(User currentUser);
    UserProfileResponse updateProfile(User currentUser, UpdateProfileRequest request);
}
