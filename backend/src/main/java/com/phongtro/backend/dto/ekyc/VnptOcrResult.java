package com.phongtro.backend.dto.ekyc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VnptOcrResult {

    private String idCardNumber;
    private String fullName;
    private String dob;
    private String sex;
    private String address;
    private String hometown;
    private String issueDate;
    private String issuePlace;
    private String cardType;
    private String rawJson;
}
