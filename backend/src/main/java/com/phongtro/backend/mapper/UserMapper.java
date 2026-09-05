package com.phongtro.backend.mapper;

import com.phongtro.backend.dto.request.RegisterRequest;
import com.phongtro.backend.dto.request.UpdateUserRequest;
import com.phongtro.backend.dto.response.UserResponse;
import com.phongtro.backend.entity.Role;
import com.phongtro.backend.entity.User;
import com.phongtro.backend.entity.UserStatus;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    /**
     * Chuyển đổi từ User Entity sang UserResponse DTO
     */
    public UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    /**
     * Chuyển đổi từ RegisterRequest DTO sang User Entity mới
     */
    public User toEntity(RegisterRequest request, String encodedPassword) {
        if (request == null) {
            return null;
        }

        // Mặc định role là TENANT nếu không được chỉ định
        Role assignedRole = request.getRole() != null ? request.getRole() : Role.TENANT;
        // Tránh trường hợp người dùng tự đăng ký với vai trò ADMIN qua API công khai
        if (assignedRole == Role.ADMIN) {
            assignedRole = Role.TENANT;
        }

        return User.builder()
                .email(request.getEmail().trim().toLowerCase())
                .passwordHash(encodedPassword)
                .fullName(request.getFullName().trim())
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .role(assignedRole)
                .status(UserStatus.ACTIVE)
                .build();
    }

    /**
     * Cập nhật các trường thông tin cho phép từ UpdateUserRequest vào Entity
     */
    public void updateEntity(User user, UpdateUserRequest request) {
        if (user == null || request == null) {
            return;
        }

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            user.setPhone(request.getPhone().trim());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }
    }
}
