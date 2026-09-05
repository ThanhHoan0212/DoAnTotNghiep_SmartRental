package com.phongtro.backend.service;

import com.phongtro.backend.common.PageResponse;
import com.phongtro.backend.dto.request.ChangePasswordRequest;
import com.phongtro.backend.dto.request.UpdateUserRequest;
import com.phongtro.backend.dto.response.UserResponse;
import com.phongtro.backend.entity.Role;
import com.phongtro.backend.entity.User;
import com.phongtro.backend.entity.UserStatus;

import java.util.UUID;

public interface UserService {

    UserResponse getCurrentUserProfile(String email);

    UserResponse updateUserProfile(String email, UpdateUserRequest request);

    void changePassword(String email, ChangePasswordRequest request);

    PageResponse<UserResponse> getAllUsers(int page, int size, Role role, UserStatus status);

    UserResponse updateUserStatus(UUID userId, UserStatus status);

    User getUserEntityByEmail(String email);

    User getUserEntityById(UUID id);
}
