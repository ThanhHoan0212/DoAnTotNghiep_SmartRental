package com.phongtro.backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Common errors
    UNCATEGORIZED_EXCEPTION("SYS_001", "Lỗi hệ thống không xác định", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY("SYS_002", "Khóa tham số không hợp lệ", HttpStatus.BAD_REQUEST),
    VALIDATION_FAILED("VAL_001", "Dữ liệu đầu vào không hợp lệ", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND("RES_001", "Không tìm thấy tài nguyên yêu cầu", HttpStatus.NOT_FOUND),

    // Authentication & Authorization errors
    UNAUTHENTICATED("AUTH_001", "Yêu cầu đăng nhập để thực hiện thao tác này", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED("AUTH_002", "Bạn không có quyền truy cập vào tài nguyên này", HttpStatus.FORBIDDEN),
    BAD_CREDENTIALS("AUTH_003", "Email hoặc mật khẩu không chính xác", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("AUTH_004", "Phiên đăng nhập đã hết hạn", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID("AUTH_005", "Token xác thực không hợp lệ", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_EXPIRED("AUTH_006", "Refresh token đã hết hạn, vui lòng đăng nhập lại", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_NOT_FOUND("AUTH_007", "Refresh token không tồn tại trong hệ thống", HttpStatus.NOT_FOUND),
    REFRESH_TOKEN_REVOKED("AUTH_008", "Refresh token đã bị thu hồi", HttpStatus.UNAUTHORIZED),

    // User errors
    USER_NOT_FOUND("USER_001", "Không tìm thấy người dùng", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS("USER_002", "Email này đã được đăng ký trong hệ thống", HttpStatus.CONFLICT),
    PHONE_ALREADY_EXISTS("USER_003", "Số điện thoại này đã được đăng ký", HttpStatus.CONFLICT),
    USER_INACTIVE("USER_004", "Tài khoản của bạn chưa được kích hoạt", HttpStatus.FORBIDDEN),
    USER_BANNED("USER_005", "Tài khoản của bạn đã bị khóa do vi phạm chính sách", HttpStatus.FORBIDDEN),
    OLD_PASSWORD_INCORRECT("USER_006", "Mật khẩu cũ không chính xác", HttpStatus.BAD_REQUEST),
    PASSWORD_RESET_TOKEN_INVALID("USER_007", "Mã xác thực đặt lại mật khẩu không hợp lệ hoặc đã hết hạn", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
