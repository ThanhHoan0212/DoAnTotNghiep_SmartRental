package com.phongtro.backend.mapper;

import com.phongtro.backend.dto.response.ContractResponse;
import com.phongtro.backend.entity.Contract;
import com.phongtro.backend.entity.RoomImage;
import org.springframework.stereotype.Component;

@Component
public class ContractMapper {

    public ContractResponse toContractResponse(Contract contract) {
        if (contract == null) return null;

        String primaryImageUrl = null;
        if (contract.getRoom() != null && contract.getRoom().getImages() != null && !contract.getRoom().getImages().isEmpty()) {
            primaryImageUrl = contract.getRoom().getImages().stream()
                    .filter(RoomImage::isPrimary)
                    .map(RoomImage::getImageUrl)
                    .findFirst()
                    .orElse(contract.getRoom().getImages().get(0).getImageUrl());
        }

        return ContractResponse.builder()
                .id(contract.getId())
                .closureRequests(new java.util.ArrayList<>(contract.getClosureRequests()))
                .agreedEndDate(contract.getAgreedEndDate())
                .terminatedAt(contract.getTerminatedAt())
                .requestCode(contract.getRequestCode())
                .depositDeadline(contract.getDepositDeadline())
                .depositPaidAt(contract.getDepositPaidAt())
                .paymentReference(contract.getPaymentReference())
                .formalizedAt(contract.getFormalizedAt())
                .tenantSignedAt(contract.getTenantSignedAt())
                .landlordSignedAt(contract.getLandlordSignedAt())
                .activatedAt(contract.getActivatedAt())
                .documentContent(contract.getDocumentContent())
                .contractCode(contract.getContractCode())
                // Room info
                .roomId(contract.getRoom() != null ? contract.getRoom().getId() : null)
                .roomTitle(contract.getRoom() != null ? contract.getRoom().getTitle() : null)
                .roomAddress(contract.getRoom() != null ? contract.getRoom().getAddress() : null)
                .roomDistrict(contract.getRoom() != null ? contract.getRoom().getDistrict() : null)
                .primaryImageUrl(primaryImageUrl)
                // Tenant info
                .tenantId(contract.getTenant() != null ? contract.getTenant().getId() : null)
                .tenantName(contract.getTenant() != null ? contract.getTenant().getFullName() : null)
                .tenantEmail(contract.getTenant() != null ? contract.getTenant().getEmail() : null)
                .tenantPhone(contract.getTenant() != null ? contract.getTenant().getPhone() : null)
                // Landlord info
                .landlordId(contract.getLandlord() != null ? contract.getLandlord().getId() : null)
                .landlordName(contract.getLandlord() != null ? contract.getLandlord().getFullName() : null)
                .landlordEmail(contract.getLandlord() != null ? contract.getLandlord().getEmail() : null)
                .landlordPhone(contract.getLandlord() != null ? contract.getLandlord().getPhone() : null)
                // Contract details
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
                .monthlyRent(contract.getMonthlyRent())
                .depositAmount(contract.getDepositAmount())
                .status(contract.getStatus())
                .terms(contract.getTerms())
                .cancellationReason(contract.getCancellationReason())
                .createdAt(contract.getCreatedAt())
                .updatedAt(contract.getUpdatedAt())
                .build();
    }
}
