package com.phongtro.backend.service;

import com.phongtro.backend.common.PageResponse;
import com.phongtro.backend.dto.AdminDtos.*;
import com.phongtro.backend.dto.response.UserResponse;
import com.phongtro.backend.entity.*;
import com.phongtro.backend.exception.AppException;
import com.phongtro.backend.exception.ErrorCode;
import com.phongtro.backend.mapper.UserMapper;
import com.phongtro.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service @RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOperationsService {
    private final SupportComplaintRepository complaints;

    private final UserRepository users;
    private final ContractRepository contracts;
    private final UserMapper userMapper;

    public static PageRequest page(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Trang phải từ 0 và kích thước trang từ 1 đến 100");
        }
        return PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }

    public PageResponse<UserResponse> users(int page, int size, Role role, UserStatus status, String keyword) {
        Specification<User> spec = (root, query, cb) -> cb.conjunction();
        if (role != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("role"), role));
        if (status != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("status"), status));
        if (keyword != null && !keyword.isBlank()) {
            String text = like(keyword);
            spec = spec.and((r, q, cb) -> cb.or(cb.like(cb.lower(r.get("email")), text, '\\'),
                    cb.like(cb.lower(r.get("fullName")), text, '\\'), cb.like(r.get("phone"), text, '\\')));
        }
        return PageResponse.from(users.findAll(spec, page(page, size)).map(userMapper::toUserResponse));
    }

    public PageResponse<ComplaintView> complaints(int page, int size, SupportComplaint.Status status, String email) {
        return complaints(page, size, status, email, null);
    }

    public PageResponse<ComplaintView> complaints(int page, int size, SupportComplaint.Status status, String email, UUID contractId) {
        Specification<SupportComplaint> spec = (r, q, cb) -> cb.conjunction();
        if (status != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("status"), status));
        if (email != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("reporter").get("email"), email));
        if (contractId != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("contract").get("id"), contractId));
        return PageResponse.from(complaints.findAll(spec, page(page, size)).map(this::view));
    }

    @Transactional
    public ComplaintView createComplaint(CreateComplaint request, String email) {
        SupportComplaint complaint = new SupportComplaint();
        User reporter = user(email);
        if (request.contractId() == null) throw new AppException(ErrorCode.VALIDATION_FAILED, "Vui lòng chọn hợp đồng cần hỗ trợ");
        Contract contract = contracts.findById(request.contractId())
                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));
        if (!contract.getTenant().getId().equals(reporter.getId()) && !contract.getLandlord().getId().equals(reporter.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Bạn chỉ có thể yêu cầu hỗ trợ cho hợp đồng của mình");
        }
        complaint.setReporter(reporter);
        complaint.setContract(contract);
        complaint.setRoom(contract.getRoom());
        complaint.setSubject(request.subject().trim());
        complaint.setCategory(request.category());
        complaint.setDescription(request.description().trim());
        return view(complaints.saveAndFlush(complaint));
    }

    @Transactional
    public ComplaintView handleComplaint(UUID id, HandleComplaint request, String email) {
        SupportComplaint complaint = complaints.findById(id).orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        if (complaint.getVersion() != request.version()) throw new OptimisticLockingFailureException("Khiếu nại đã thay đổi");
        if (request.status() == SupportComplaint.Status.OPEN) throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED,
                "Chọn đang xử lý, đã giải quyết hoặc từ chối khi phản hồi khiếu nại");
        complaint.setStatus(request.status());
        complaint.setResponse(request.response().trim());
        complaint.setHandledBy(user(email));
        return view(complaints.saveAndFlush(complaint));
    }

    private User user(String email) {
        return users.findByEmail(email).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
    private static String like(String keyword) {
        return "%" + keyword.trim().toLowerCase(java.util.Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }
    private ComplaintView view(SupportComplaint c) {
        return new ComplaintView(c.getId(), c.getReporter().getId(), c.getReporter().getFullName(), c.getReporter().getEmail(),
                c.getRoom() == null ? null : c.getRoom().getId(), c.getRoom() == null ? null : c.getRoom().getTitle(),
                c.getContract() == null ? null : c.getContract().getId(), c.getContract() == null ? null : c.getContract().getContractCode(),
                c.getSubject(), c.getCategory(), c.getDescription(), c.getStatus(), c.getResponse(),
                c.getHandledBy() == null ? null : c.getHandledBy().getFullName(), c.getVersion(), c.getCreatedAt(), c.getUpdatedAt());
    }

}
