package com.phongtro.backend.service;

import com.phongtro.backend.dto.AdminDtos.*;
import com.phongtro.backend.entity.*;
import com.phongtro.backend.exception.AppException;
import com.phongtro.backend.mapper.UserMapper;
import com.phongtro.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminOperationsServiceTest {
    @Mock SupportComplaintRepository complaints;

    @Mock UserRepository users;
    @Mock ContractRepository contracts;
    @Mock UserMapper userMapper;
    @InjectMocks AdminOperationsService service;

    private User user(String email) {
        return User.builder().id(UUID.randomUUID()).email(email).fullName("Người kiểm thử").role(Role.TENANT).status(UserStatus.ACTIVE).build();
    }
    @Test void newComplaintAlwaysStartsOpenAndUsesAuthenticatedReporter() {
        User reporter = user("tenant@test.local");
        when(users.findByEmail(reporter.getEmail())).thenReturn(Optional.of(reporter));
        Contract contract = Contract.builder().id(UUID.randomUUID()).contractCode("HD-TEST")
                .tenant(reporter).landlord(user("landlord@test.local")).room(Room.builder().id(UUID.randomUUID()).title("Phòng thử").build()).build();
        when(contracts.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(complaints.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        ComplaintView result = service.createComplaint(new CreateComplaint("Tin không chính xác", SupportComplaint.Category.LISTING, "Địa chỉ trong tin đăng không đúng", contract.getId()), reporter.getEmail());
        assertEquals(reporter.getId(), result.reporterId());
        assertEquals(SupportComplaint.Status.OPEN, result.status());
        assertNull(result.handledBy());
        assertEquals(contract.getId(), result.contractId());
        assertEquals(contract.getRoom().getId(), result.roomId());
    }
    @Test void complaintRejectsUnknownContract() {
        User reporter = user("tenant@test.local");
        when(users.findByEmail(reporter.getEmail())).thenReturn(Optional.of(reporter));
        when(contracts.findById(any())).thenReturn(Optional.empty());
        assertThrows(AppException.class, () -> service.createComplaint(new CreateComplaint("Tiêu đề", SupportComplaint.Category.FRAUD, "Mô tả chi tiết", UUID.randomUUID()), reporter.getEmail()));
        verify(complaints, never()).saveAndFlush(any());
    }
    @Test void unrelatedUserCannotRequestSupportForAnotherContract() {
        User reporter = user("outsider@test.local");
        when(users.findByEmail(reporter.getEmail())).thenReturn(Optional.of(reporter));
        Contract contract = Contract.builder().id(UUID.randomUUID()).tenant(user("tenant@test.local"))
                .landlord(user("landlord@test.local")).build();
        when(contracts.findById(contract.getId())).thenReturn(Optional.of(contract));
        assertThrows(AppException.class, () -> service.createComplaint(new CreateComplaint("Tiêu đề", SupportComplaint.Category.OTHER,
                "Yêu cầu không được phép", contract.getId()), reporter.getEmail()));
        verify(complaints, never()).saveAndFlush(any());
    }
    @Test void complaintRequiresContract() {
        User reporter = user("tenant@test.local");
        when(users.findByEmail(reporter.getEmail())).thenReturn(Optional.of(reporter));
        assertThrows(AppException.class, () -> service.createComplaint(new CreateComplaint("Tiêu đề", SupportComplaint.Category.OTHER,
                "Nội dung cần hỗ trợ", null), reporter.getEmail()));
        verifyNoInteractions(contracts);
        verify(complaints, never()).saveAndFlush(any());
    }
    @Test void staleComplaintUpdateDoesNotOverwriteResponse() {
        UUID id = UUID.randomUUID(); SupportComplaint c = new SupportComplaint(); c.setVersion(2); c.setResponse("Phản hồi mới");
        when(complaints.findById(id)).thenReturn(Optional.of(c));
        assertThrows(OptimisticLockingFailureException.class, () -> service.handleComplaint(id, new HandleComplaint(SupportComplaint.Status.RESOLVED, "Phản hồi cũ", 1), "admin@test.local"));
        assertEquals("Phản hồi mới", c.getResponse());
        verify(complaints, never()).saveAndFlush(any());
    }
    @Test void handlingComplaintRecordsAdminAndResponse() {
        UUID id = UUID.randomUUID(); SupportComplaint c = new SupportComplaint(); c.setReporter(user("tenant@test.local"));
        User admin = user("admin@test.local");
        when(complaints.findById(id)).thenReturn(Optional.of(c));
        when(users.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(complaints.saveAndFlush(c)).thenReturn(c);
        ComplaintView result = service.handleComplaint(id, new HandleComplaint(SupportComplaint.Status.RESOLVED, "Đã kiểm tra và xử lý", 0), admin.getEmail());
        assertEquals(SupportComplaint.Status.RESOLVED, result.status());
        assertEquals(admin, c.getHandledBy());
        assertEquals("Đã kiểm tra và xử lý", result.response());
    }
    @Test void paginationIsBounded() {
        assertThrows(AppException.class, () -> AdminOperationsService.page(-1, 10));
        assertThrows(AppException.class, () -> AdminOperationsService.page(0, 101));
        assertThrows(AppException.class, () -> AdminOperationsService.page(0, 0));
        assertEquals(100, AdminOperationsService.page(0, 100).getPageSize());
    }
}
