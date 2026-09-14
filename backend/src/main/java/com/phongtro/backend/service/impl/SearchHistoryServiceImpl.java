package com.phongtro.backend.service.impl;

import com.phongtro.backend.dto.request.SaveSearchHistoryRequest;
import com.phongtro.backend.dto.response.SearchHistoryResponse;
import com.phongtro.backend.entity.SearchHistory;
import com.phongtro.backend.entity.User;
import com.phongtro.backend.exception.AppException;
import com.phongtro.backend.exception.ErrorCode;
import com.phongtro.backend.repository.SearchHistoryRepository;
import com.phongtro.backend.repository.UserRepository;
import com.phongtro.backend.service.SearchHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchHistoryServiceImpl implements SearchHistoryService {

    private final SearchHistoryRepository searchHistoryRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void saveSearchHistory(SaveSearchHistoryRequest request, String userEmail) {
        if (request == null || (isEmpty(request.getQueryText()) && isEmpty(request.getDistrict())
                && request.getMinPrice() == null && request.getMaxPrice() == null)) {
            return;
        }

        User user = getUserByEmail(userEmail);

        SearchHistory history = SearchHistory.builder()
                .user(user)
                .queryText(request.getQueryText() != null ? request.getQueryText().trim() : null)
                .district(request.getDistrict() != null ? request.getDistrict().trim() : null)
                .minPrice(request.getMinPrice())
                .maxPrice(request.getMaxPrice())
                .build();

        searchHistoryRepository.save(history);
        log.info("Saved search history for user [{}]: query=[{}]", userEmail, request.getQueryText());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SearchHistoryResponse> getRecentSearches(String userEmail) {
        User user = getUserByEmail(userEmail);
        List<SearchHistory> histories = searchHistoryRepository.findTop10ByUserOrderBySearchedAtDesc(user);

        if (histories == null || histories.isEmpty()) {
            return Collections.emptyList();
        }

        return histories.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteSearchHistoryItem(UUID id, String userEmail) {
        User user = getUserByEmail(userEmail);
        SearchHistory history = searchHistoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!history.getUser().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        searchHistoryRepository.delete(history);
        log.info("User [{}] deleted search history item [{}]", userEmail, id);
    }

    @Override
    @Transactional
    public void clearAllSearchHistory(String userEmail) {
        User user = getUserByEmail(userEmail);
        searchHistoryRepository.deleteByUser(user);
        log.info("Cleared all search history for user [{}]", userEmail);
    }

    private SearchHistoryResponse toResponse(SearchHistory entity) {
        return SearchHistoryResponse.builder()
                .id(entity.getId())
                .queryText(entity.getQueryText())
                .district(entity.getDistrict())
                .minPrice(entity.getMinPrice())
                .maxPrice(entity.getMaxPrice())
                .searchedAt(entity.getSearchedAt())
                .build();
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private boolean isEmpty(String str) {
        return str == null || str.trim().isBlank();
    }
}
