package com.phongtro.backend.service.parser;

import com.phongtro.backend.dto.response.ParsedQueryResponse;
import com.phongtro.backend.entity.Amenity;
import com.phongtro.backend.repository.AmenityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class RuleBasedQueryParser {

    private final AmenityRepository amenityRepository;

    private static final List<String> HCM_DISTRICTS = List.of(
            "Quận 1", "Quận 3", "Quận 4", "Quận 5", "Quận 6", "Quận 7", "Quận 8", "Quận 10", "Quận 11", "Quận 12",
            "Bình Thạnh", "Gò Vấp", "Phú Nhuận", "Tân Bình", "Tân Phú", "Bình Tân",
            "Thủ Đức", "Nhà Bè", "Hóc Môn", "Bình Chánh", "Củ Chi", "Cần Giờ"
    );

    // Map các alias viết tắt phổ biến ở TP.HCM (q1, q.1, thu duc, ...) sang tên chuẩn
    private static final Map<String, String> DISTRICT_ALIASES = Map.ofEntries(
            Map.entry("q1", "Quận 1"),
            Map.entry("q.1", "Quận 1"),
            Map.entry("quan 1", "Quận 1"),
            Map.entry("q3", "Quận 3"),
            Map.entry("q.3", "Quận 3"),
            Map.entry("quan 3", "Quận 3"),
            Map.entry("q4", "Quận 4"),
            Map.entry("q.4", "Quận 4"),
            Map.entry("quan 4", "Quận 4"),
            Map.entry("q5", "Quận 5"),
            Map.entry("q.5", "Quận 5"),
            Map.entry("quan 5", "Quận 5"),
            Map.entry("q6", "Quận 6"),
            Map.entry("q.6", "Quận 6"),
            Map.entry("quan 6", "Quận 6"),
            Map.entry("q7", "Quận 7"),
            Map.entry("q.7", "Quận 7"),
            Map.entry("quan 7", "Quận 7"),
            Map.entry("q8", "Quận 8"),
            Map.entry("q.8", "Quận 8"),
            Map.entry("quan 8", "Quận 8"),
            Map.entry("q10", "Quận 10"),
            Map.entry("q.10", "Quận 10"),
            Map.entry("quan 10", "Quận 10"),
            Map.entry("q11", "Quận 11"),
            Map.entry("q.11", "Quận 11"),
            Map.entry("quan 11", "Quận 11"),
            Map.entry("q12", "Quận 12"),
            Map.entry("q.12", "Quận 12"),
            Map.entry("quan 12", "Quận 12"),
            Map.entry("tp thu duc", "Thủ Đức"),
            Map.entry("thanh pho thu duc", "Thủ Đức")
    );

    private static final Map<String, String> AMENITY_KEYWORDS = Map.ofEntries(
            Map.entry("dieu hoa", "Điều hòa nhiệt độ"),
            Map.entry("may lanh", "Điều hòa nhiệt độ"),
            Map.entry("nong lanh", "Bình nóng lạnh"),
            Map.entry("binh nong", "Bình nóng lạnh"),
            Map.entry("may giat", "Máy giặt chung"),
            Map.entry("tu lanh", "Tủ lạnh"),
            Map.entry("ban cong", "Ban công thoáng mát"),
            Map.entry("bep", "Khu vực bếp riêng"),
            Map.entry("khep kin", "Vệ sinh khép kín"),
            Map.entry("wifi", "Wifi tốc độ cao"),
            Map.entry("de xe", "Chỗ để xe miễn phí"),
            Map.entry("gui xe", "Chỗ để xe miễn phí"),
            Map.entry("tu do", "Giờ giấc tự do"),
            Map.entry("khong chung chu", "Giờ giấc tự do")
    );

    public ParsedQueryResponse parse(String rawQuery) {
        if (rawQuery == null || rawQuery.trim().isBlank()) {
            return ParsedQueryResponse.builder()
                    .source("RULE_BASED_FALLBACK")
                    .originalQuery("")
                    .build();
        }

        String query = rawQuery.trim();
        String normalized = removeAccents(query.toLowerCase());

        String detectedDistrict = extractDistrict(query, normalized);
        Double[] prices = extractPrices(normalized);
        Double minPrice = prices[0];
        Double maxPrice = prices[1];

        Double[] areas = extractAreas(normalized);
        Double minArea = areas[0];
        Double maxArea = areas[1];

        List<String> detectedAmenityNames = extractAmenities(normalized);
        List<Long> amenityIds = resolveAmenityIds(detectedAmenityNames);

        // Trích xuất từ khóa phụ (ví dụ: gác lửng, mini, studio, chung cư mini...)
        String keyword = extractKeywords(normalized);

        log.info("RuleBasedQueryParser parsed query: district=[{}], price=[{} - {}], amenities={}",
                detectedDistrict, minPrice, maxPrice, detectedAmenityNames);

        return ParsedQueryResponse.builder()
                .originalQuery(rawQuery)
                .keyword(keyword)
                .district(detectedDistrict)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .minArea(minArea)
                .maxArea(maxArea)
                .detectedAmenities(detectedAmenityNames)
                .amenityIds(amenityIds)
                .source("RULE_BASED_FALLBACK")
                .build();
    }

    private String extractDistrict(String rawQuery, String normalized) {
        // 1. Kiểm tra các alias viết tắt trước (vd: q1, q.1, quan 1, tp thu duc...)
        for (Map.Entry<String, String> entry : DISTRICT_ALIASES.entrySet()) {
            Pattern p = Pattern.compile("(?:^|\\s|\\b)" + Pattern.quote(entry.getKey()) + "(?:$|\\s|\\b)");
            if (p.matcher(normalized).find() || normalized.contains(" " + entry.getKey() + " ")) {
                return entry.getValue();
            }
        }

        // 2. Kiểm tra danh sách các quận huyện TP.HCM
        for (String district : HCM_DISTRICTS) {
            String districtNorm = removeAccents(district.toLowerCase());
            if (normalized.contains(districtNorm)) {
                return district;
            }
        }
        return null;
    }

    private Double[] extractPrices(String text) {
        Double min = null;
        Double max = null;

        // Pattern: "tu X den Y trieu" / "tu X - Y tr"
        Pattern rangePattern = Pattern.compile("(?:tu\\s*)?(\\d+(?:[.,]\\d+)?)\\s*(?:den|-|toi)\\s*(\\d+(?:[.,]\\d+)?)\\s*(?:trieu|tr|k|dong|d)?");
        Matcher rangeMatcher = rangePattern.matcher(text);
        if (rangeMatcher.find()) {
            try {
                min = parsePriceValue(rangeMatcher.group(1));
                max = parsePriceValue(rangeMatcher.group(2));
                return new Double[]{min, max};
            } catch (Exception ignored) {}
        }

        // Pattern: "duoi X trieu" / "toi da X tr" / "< X trieu"
        Pattern underPattern = Pattern.compile("(?:duoi|nho hon|toi da|<)\\s*(\\d+(?:[.,]\\d+)?)\\s*(?:trieu|tr|dong|d)?");
        Matcher underMatcher = underPattern.matcher(text);
        if (underMatcher.find()) {
            try {
                max = parsePriceValue(underMatcher.group(1));
            } catch (Exception ignored) {}
        }

        // Pattern: "tren X trieu" / "tu X tr tro len" / "> X trieu"
        Pattern abovePattern = Pattern.compile("(?:tren|lon hon|tu|>)\\s*(\\d+(?:[.,]\\d+)?)\\s*(?:trieu|tr|dong|d)?");
        Matcher aboveMatcher = abovePattern.matcher(text);
        if (aboveMatcher.find() && max == null) {
            try {
                min = parsePriceValue(aboveMatcher.group(1));
            } catch (Exception ignored) {}
        }

        // Pattern: "tam X trieu" / "khoang X tr"
        Pattern approxPattern = Pattern.compile("(?:tam|khoang|gia)\\s*(\\d+(?:[.,]\\d+)?)\\s*(?:trieu|tr)");
        Matcher approxMatcher = approxPattern.matcher(text);
        if (approxMatcher.find() && min == null && max == null) {
            try {
                double target = parsePriceValue(approxMatcher.group(1));
                min = Math.max(0, target - 500000);
                max = target + 500000;
            } catch (Exception ignored) {}
        }

        return new Double[]{min, max};
    }

    private Double[] extractAreas(String text) {
        Double min = null;
        Double max = null;

        Pattern areaUnder = Pattern.compile("(?:duoi|<)\\s*(\\d+(?:[.,]\\d+)?)\\s*(?:m2|met vuong)");
        Matcher underM = areaUnder.matcher(text);
        if (underM.find()) {
            try {
                max = Double.parseDouble(underM.group(1).replace(',', '.'));
            } catch (Exception ignored) {}
        }

        Pattern areaAbove = Pattern.compile("(?:tren|>|tu)\\s*(\\d+(?:[.,]\\d+)?)\\s*(?:m2|met vuong)");
        Matcher aboveM = areaAbove.matcher(text);
        if (aboveM.find()) {
            try {
                min = Double.parseDouble(aboveM.group(1).replace(',', '.'));
            } catch (Exception ignored) {}
        }

        return new Double[]{min, max};
    }

    private double parsePriceValue(String str) {
        double val = Double.parseDouble(str.replace(',', '.'));
        if (val < 100) {
            // Đơn vị triệu (vd: 3.5 -> 3,500,000)
            return val * 1000000.0;
        } else if (val < 10000) {
            // Đơn vị nghìn (vd: 3500 -> 3,500,000)
            return val * 1000.0;
        }
        return val;
    }

    private List<String> extractAmenities(String text) {
        Set<String> result = new LinkedHashSet<>();
        AMENITY_KEYWORDS.forEach((kw, officialName) -> {
            if (text.contains(kw)) {
                result.add(officialName);
            }
        });
        return new ArrayList<>(result);
    }

    private List<Long> resolveAmenityIds(List<String> amenityNames) {
        if (amenityNames == null || amenityNames.isEmpty()) {
            return Collections.emptyList();
        }
        List<Amenity> all = amenityRepository.findAll();
        List<Long> ids = new ArrayList<>();
        for (String name : amenityNames) {
            for (Amenity a : all) {
                if (a.getName().equalsIgnoreCase(name)) {
                    ids.add(a.getId());
                    break;
                }
            }
        }
        return ids;
    }

    private String extractKeywords(String text) {
        List<String> specialKeywords = List.of("gac lung", "studio", "chung cu mini", "can ho mini", "nguyen can", "o ghep");
        for (String kw : specialKeywords) {
            if (text.contains(kw)) {
                return kw;
            }
        }
        return null;
    }

    private String removeAccents(String s) {
        if (s == null) return "";
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("")
                .replace("đ", "d")
                .replace("Đ", "D");
    }
}
