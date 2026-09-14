package com.phongtro.backend.repository;

import com.phongtro.backend.entity.SearchHistory;
import com.phongtro.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, UUID> {

    List<SearchHistory> findTop10ByUserOrderBySearchedAtDesc(User user);

    Page<SearchHistory> findByUserOrderBySearchedAtDesc(User user, Pageable pageable);

    @Modifying
    @Query("DELETE FROM SearchHistory s WHERE s.user = :user")
    void deleteByUser(@Param("user") User user);
}
