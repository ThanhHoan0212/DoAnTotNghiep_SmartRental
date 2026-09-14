package com.phongtro.backend.service;

import com.phongtro.backend.dto.request.SaveSearchHistoryRequest;
import com.phongtro.backend.dto.response.SearchHistoryResponse;
import com.phongtro.backend.entity.Role;
import com.phongtro.backend.entity.SearchHistory;
import com.phongtro.backend.entity.User;
import com.phongtro.backend.entity.UserStatus;
import com.phongtro.backend.exception.AppException;
import com.phongtro.backend.exception.ErrorCode;
import com.phongtro.backend.repository.SearchHistoryRepository;
import com.phongtro.backend.repository.UserRepository;
import com.phongtro.backend.service.impl.SearchHistoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchHistoryServiceTest {

    @Mock
    private SearchHistoryRepository searchHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SearchHistoryServiceImpl searchHistoryService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(UUID.randomUUID())
                .email("tenant@phongtro.vn")
                .fullName("Nguyễn Văn A")
                .role(Role.TENANT)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Lưu lịch sử tìm kiếm: dữ liệu hợp lệ -> lưu thành công")
    void saveSearchHistory_WhenValidRequest_ShouldSave() {
        when(userRepository.findByEmail(sampleUser.getEmail())).thenReturn(Optional.of(sampleUser));

        SaveSearchHistoryRequest request = SaveSearchHistoryRequest.builder()
                .queryText("phòng trọ giá rẻ có gác")
                .district("Bình Thạnh")
                .minPrice(2000000.0)
                .maxPrice(5000000.0)
                .build();

        searchHistoryService.saveSearchHistory(request, sampleUser.getEmail());

        verify(searchHistoryRepository, times(1)).save(any(SearchHistory.class));
    }

    @Test
    @DisplayName("Lưu lịch sử tìm kiếm: yêu cầu rỗng -> bỏ qua không lưu")
    void saveSearchHistory_WhenEmptyRequest_ShouldDoNothing() {
        SaveSearchHistoryRequest request = SaveSearchHistoryRequest.builder()
                .queryText("   ")
                .district(null)
                .build();

        searchHistoryService.saveSearchHistory(request, sampleUser.getEmail());

        verify(searchHistoryRepository, never()).save(any(SearchHistory.class));
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    @DisplayName("Lấy danh sách top 10 tìm kiếm gần đây")
    void getRecentSearches_ShouldReturnList() {
        when(userRepository.findByEmail(sampleUser.getEmail())).thenReturn(Optional.of(sampleUser));

        SearchHistory history = SearchHistory.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .queryText("phòng trọ Quận 10")
                .district("Quận 10")
                .searchedAt(Instant.now())
                .build();

        when(searchHistoryRepository.findTop10ByUserOrderBySearchedAtDesc(sampleUser))
                .thenReturn(List.of(history));

        List<SearchHistoryResponse> result = searchHistoryService.getRecentSearches(sampleUser.getEmail());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("phòng trọ Quận 10", result.get(0).getQueryText());
        assertEquals("Quận 10", result.get(0).getDistrict());
    }

    @Test
    @DisplayName("Xóa một mục tìm kiếm: chủ sở hữu thực hiện -> xóa thành công")
    void deleteSearchHistoryItem_WhenOwner_ShouldDelete() {
        UUID historyId = UUID.randomUUID();
        SearchHistory history = SearchHistory.builder()
                .id(historyId)
                .user(sampleUser)
                .queryText("phòng trọ Quận 1")
                .build();

        when(userRepository.findByEmail(sampleUser.getEmail())).thenReturn(Optional.of(sampleUser));
        when(searchHistoryRepository.findById(historyId)).thenReturn(Optional.of(history));

        searchHistoryService.deleteSearchHistoryItem(historyId, sampleUser.getEmail());

        verify(searchHistoryRepository, times(1)).delete(history);
    }

    @Test
    @DisplayName("Xóa một mục tìm kiếm: không phải người tạo -> ném lỗi UNAUTHORIZED")
    void deleteSearchHistoryItem_WhenNotOwner_ShouldThrowUnauthorized() {
        UUID historyId = UUID.randomUUID();
        User otherUser = User.builder()
                .id(UUID.randomUUID())
                .email("other@phongtro.vn")
                .build();

        SearchHistory history = SearchHistory.builder()
                .id(historyId)
                .user(otherUser)
                .queryText("phòng trọ")
                .build();

        when(userRepository.findByEmail(sampleUser.getEmail())).thenReturn(Optional.of(sampleUser));
        when(searchHistoryRepository.findById(historyId)).thenReturn(Optional.of(history));

        AppException exception = assertThrows(AppException.class, () ->
                searchHistoryService.deleteSearchHistoryItem(historyId, sampleUser.getEmail()));

        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
        verify(searchHistoryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Xóa toàn bộ lịch sử tìm kiếm của người dùng hiện tại")
    void clearAllSearchHistory_ShouldCallDeleteByUser() {
        when(userRepository.findByEmail(sampleUser.getEmail())).thenReturn(Optional.of(sampleUser));

        searchHistoryService.clearAllSearchHistory(sampleUser.getEmail());

        verify(searchHistoryRepository, times(1)).deleteByUser(sampleUser);
    }
}
