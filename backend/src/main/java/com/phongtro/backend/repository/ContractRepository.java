package com.phongtro.backend.repository;

import com.phongtro.backend.entity.Contract;
import com.phongtro.backend.entity.ContractStatus;
import com.phongtro.backend.entity.Room;
import com.phongtro.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContractRepository extends JpaRepository<Contract, UUID> {

    @org.springframework.data.jpa.repository.Query("select c.id from Contract c where (c.status = com.phongtro.backend.entity.ContractStatus.ACTIVE and (c.endDate < :today or c.agreedEndDate <= :today)) or (c.status = com.phongtro.backend.entity.ContractStatus.AWAITING_DEPOSIT and c.depositDeadline <= :now) order by c.endDate, c.id")
    java.util.List<UUID> findExpiredActiveIds(@org.springframework.data.repository.query.Param("today") java.time.LocalDate today, @org.springframework.data.repository.query.Param("now") java.time.Instant now, Pageable pageable);

    Optional<Contract> findByContractCode(String contractCode);

    Page<Contract> findByTenantOrderByCreatedAtDesc(User tenant, Pageable pageable);

    Page<Contract> findByTenantAndStatusOrderByCreatedAtDesc(User tenant, ContractStatus status, Pageable pageable);

    Page<Contract> findByLandlordOrderByCreatedAtDesc(User landlord, Pageable pageable);

    Page<Contract> findByLandlordAndStatusOrderByCreatedAtDesc(User landlord, ContractStatus status, Pageable pageable);

    Page<Contract> findByLandlordAndRoomIdOrderByCreatedAtDesc(User landlord, UUID roomId, Pageable pageable);

    Page<Contract> findByLandlordAndRoomIdAndStatusOrderByCreatedAtDesc(User landlord, UUID roomId, ContractStatus status, Pageable pageable);

    Page<Contract> findByStatusOrderByCreatedAtDesc(ContractStatus status, Pageable pageable);

    Page<Contract> findAllByOrderByCreatedAtDesc(Pageable pageable);

    boolean existsByTenantAndRoomAndStatusIn(User tenant, Room room, Collection<ContractStatus> statuses);

    boolean existsByRoomAndStatus(Room room, ContractStatus status);

    boolean existsByRoomAndStatusAndIdNot(Room room, ContractStatus status, UUID id);

    boolean existsByRoom(Room room);

    boolean existsByRoomAndStatusIn(Room room, Collection<ContractStatus> statuses);

    java.util.List<Contract> findByRoomAndStatusAndIdNot(Room room, ContractStatus status, UUID excludeId);

    long countByLandlordAndStatus(User landlord, ContractStatus status);
}
