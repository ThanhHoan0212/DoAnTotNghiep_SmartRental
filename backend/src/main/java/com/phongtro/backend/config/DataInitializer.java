package com.phongtro.backend.config;

import com.phongtro.backend.entity.Role;
import com.phongtro.backend.entity.User;
import com.phongtro.backend.entity.UserStatus;
import com.phongtro.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        userRepository.findByEmail("admin@phongtro.vn").ifPresentOrElse(
                admin -> {
                    if (!passwordEncoder.matches("Admin@123", admin.getPasswordHash())) {
                        log.info("Cập nhật lại mật khẩu băm chuẩn cho tài khoản Admin: admin@phongtro.vn");
                        admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
                        admin.setStatus(UserStatus.ACTIVE);
                        userRepository.save(admin);
                        log.info("Đã cập nhật mật khẩu thành công cho admin@phongtro.vn");
                    }
                },
                () -> {
                    log.info("Khởi tạo tài khoản Admin mặc định: admin@phongtro.vn");
                    User admin = User.builder()
                            .email("admin@phongtro.vn")
                            .passwordHash(passwordEncoder.encode("Admin@123"))
                            .fullName("Hệ Thống Quản Trị Viên")
                            .phone("0900000000")
                            .role(Role.ADMIN)
                            .status(UserStatus.ACTIVE)
                            .build();
                    userRepository.save(admin);
                    log.info("Đã khởi tạo tài khoản admin@phongtro.vn thành công");
                }
        );
    }
}
