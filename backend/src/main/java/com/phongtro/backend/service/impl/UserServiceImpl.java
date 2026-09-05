package com.phongtro.backend.service.impl;

import com.phongtro.backend.common.PageResponse;
import com.phongtro.backend.dto.request.ChangePasswordRequest;
import com.phongtro.backend.dto.request.UpdateUserRequest;
import com.phongtro.backend.dto.response.UserResponse;
import com.phongtro.backend.entity.Role;
import com.phongtro.backend.entity.User;
import com.phongtro.backend.entity.UserStatus;
import com.phongtro.backend.exception.AppException;
import com.phongtro.backend.exception.ErrorCode;
import com.phongtro.backend.mapper.UserMapper;
import com.phongtro.backend.repository.UserRepository;
import com.phongtro.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile(String email) {
        User user = getUserEntityByEmail(email);
        return userMapper.toUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUserProfile(String email, UpdateUserRequest request) {
        User user = getUserEntityByEmail(email);

        // Kiểm tra số điện thoại mới có bị trùng với người dùng khác hay không
        if (request.getPhone() != null && !request.getPhone().isBlank()
                && !request.getPhone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(request.getPhone())) {
                throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS);
            }
        }

        userMapper.updateEntity(user, request);
        User savedUser = userRepository.save(user);
        log.info("User [{}] updated profile successfully", email);
        return userMapper.toUserResponse(savedUser);
    }

    @Override
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = getUserEntityByEmail(email);

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.OLD_PASSWORD_INCORRECT);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("User [{}] changed password successfully", email);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(int page, int size, Role role, UserStatus status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> usersPage;

        if (role != null && status != null) {
            usersPage = userRepository.findByRoleAndStatus(role, status, pageable);
        } else if (role != null) {
            usersPage = userRepository.findByRole(role, pageable);
        } else if (status != null) {
            usersPage = userRepository.findByStatus(status, pageable);
        } else {
            usersPage = userRepository.findAll(pageable);
        }

        Page<UserResponse> dtoPage = usersPage.map(userMapper::toUserResponse);
        return PageResponse.from(dtoPage);
    }

    @Override
    @Transactional
    public UserResponse updateUserStatus(UUID userId, UserStatus status) {
        User user = getUserEntityById(userId);
        user.setStatus(status);
        User updatedUser = userRepository.save(user);
        log.info("Admin updated user [{}] status to [{}]", userId, status);
        return userMapper.toUserResponse(updatedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserEntityByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserEntityById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}
