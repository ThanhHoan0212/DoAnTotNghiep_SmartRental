import logging
import re
import unicodedata
from typing import Any, Dict, List, Optional, Tuple

from app.schemas.nlp import ParsedFilters, ParseQueryRequest, ParseQueryResponse

logger = logging.getLogger("ai_service.nlp")


def remove_accents(input_str: str) -> str:
    """Loại bỏ dấu tiếng Việt để đối chiếu từ khóa không phân biệt dấu"""
    if not input_str:
        return ""
    nfkd = unicodedata.normalize("NFKD", input_str)
    no_accents = "".join([c for c in nfkd if not unicodedata.combining(c)])
    return no_accents.replace("đ", "d").replace("Đ", "D")


DISTRICT_ALIASES: Dict[str, str] = {
    # Quận trung tâm
    "q1": "Quận 1", "q.1": "Quận 1", "quan 1": "Quận 1", "quan nhat": "Quận 1", "quận nhất": "Quận 1",
    "q2": "Quận 2", "q.2": "Quận 2", "quan 2": "Quận 2",
    "q3": "Quận 3", "q.3": "Quận 3", "quan 3": "Quận 3",
    "q4": "Quận 4", "q.4": "Quận 4", "quan 4": "Quận 4",
    "q5": "Quận 5", "q.5": "Quận 5", "quan 5": "Quận 5",
    "q6": "Quận 6", "q.6": "Quận 6", "quan 6": "Quận 6",
    "q7": "Quận 7", "q.7": "Quận 7", "quan 7": "Quận 7",
    "q8": "Quận 8", "q.8": "Quận 8", "quan 8": "Quận 8",
    "q9": "Quận 9", "q.9": "Quận 9", "quan 9": "Quận 9",
    "q10": "Quận 10", "q.10": "Quận 10", "quan 10": "Quận 10",
    "q11": "Quận 11", "q.11": "Quận 11", "quan 11": "Quận 11",
    "q12": "Quận 12", "q.12": "Quận 12", "quan 12": "Quận 12",
    # Các quận huyện tên chữ
    "binh thanh": "Bình Thạnh", "bt": "Bình Thạnh",
    "go vap": "Gò Vấp", "gv": "Gò Vấp",
    "phu nhuan": "Phú Nhuận", "pn": "Phú Nhuận",
    "tan binh": "Tân Bình",
    "tan phu": "Tân Phú",
    "binh tan": "Bình Tân",
    "tp thu duc": "Thủ Đức", "thanh pho thu duc": "Thủ Đức", "thu duc": "Thủ Đức",
    "nha be": "Nhà Bè",
    "hoc mon": "Hóc Môn",
    "binh chanh": "Bình Chánh",
    "cu chi": "Củ Chi",
    "can gio": "Cần Giờ"
}

# Tiện ích chuẩn hóa và các từ khóa liên quan
AMENITY_RULES: List[Tuple[str, int, List[str]]] = [
    ("chỗ để xe", 1, ["de xe", "cho de xe", "gui xe", "bai xe", "cho dau xe"]),
    ("ban công", 2, ["ban cong", "cua so", "ban cong thoang"]),
    ("máy lạnh", 3, ["may lanh", "dieu hoa", "may lanh tiet kiem"]),
    ("máy giặt", 4, ["may giat", "may giat chung", "may giat rieng"]),
    ("tủ lạnh", 5, ["tu lanh"]),
    ("gác lửng", 6, ["gac lung", "gac xep", "co gac", "gac"]),
    ("vệ sinh khép kín", 7, ["khep kin", "ve sinh khep kin", "wc rieng", "ve sinh rieng"]),
    ("khu vực bếp", 8, ["bep", "ke bep", "nau an", "bep rieng"]),
    ("giờ giấc tự do", 9, ["gio giac tu do", "tu do", "khong chung chu", "gio tu do"]),
    ("wifi miễn phí", 10, ["wifi", "mang", "internet"]),
    ("cho nuôi thú cưng", 11, ["nuoi thu cung", "thu cung", "pet", "cho meo", "nuoi meo", "nuoi cho"]),
    ("thang máy", 12, ["thang may"]),
    ("bảo vệ 24/7", 13, ["bao ve", "an ninh", "camera", "khoa van tay"]),
    ("bình nóng lạnh", 14, ["nong lanh", "binh nong lanh", "nuoc nong"])
]

ROOM_TYPE_KEYWORDS = [
    ("chung cư mini", ["chung cu mini", "ccmn"]),
    ("studio", ["studio"]),
    ("căn hộ dịch vụ", ["can ho dich vu", "chdv"]),
    ("phòng trọ", ["phong tro", "nha tro"]),
    ("ở ghép", ["o ghep", "tim ban o ghep"])
]


class NlpQueryParserService:
    def parse_query(self, query: str) -> ParseQueryResponse:
        """
        Phân tích câu truy vấn tự nhiên tiếng Việt và trích xuất các thực thể tìm kiếm:
        1. Khoảng giá (min_price, max_price)
        2. Khu vực (Quận / Huyện)
        3. Diện tích (min_area, max_area)
        4. Tiện ích (amenities)
        5. Loại phòng / Từ khóa
        6. Điểm tin cậy (confidence)
        """
        raw_text = query.strip()
        lower_raw = raw_text.lower()
        normalized = remove_accents(lower_raw)

        # 1. Trích xuất Quận/Huyện
        detected_district = self._extract_district(normalized)

        # 2. Trích xuất Khoảng giá
        min_price, max_price = self._extract_price(normalized, lower_raw)

        # 3. Trích xuất Diện tích
        min_area, max_area = self._extract_area(normalized)

        # 4. Trích xuất Tiện ích
        detected_amenities, amenity_ids = self._extract_amenities(normalized)

        # 5. Trích xuất Loại phòng / Keyword
        keyword = self._extract_keyword(normalized)

        # 6. Tính toán Độ tin cậy (Confidence)
        confidence = self._calculate_confidence(
            district=detected_district,
            min_price=min_price,
            max_price=max_price,
            min_area=min_area,
            amenities=detected_amenities,
            keyword=keyword
        )

        filters = ParsedFilters(
            max_price=max_price,
            min_price=min_price,
            district=detected_district,
            amenities=detected_amenities,
            min_area=min_area,
            max_area=max_area
        )

        return ParseQueryResponse(
            success=True,
            originalQuery=raw_text,
            keyword=keyword,
            district=detected_district,
            minPrice=min_price,
            maxPrice=max_price,
            minArea=min_area,
            maxArea=max_area,
            detectedAmenities=detected_amenities,
            amenityIds=amenity_ids,
            confidence=round(confidence, 2),
            source="AI_SERVICE",
            filters=filters
        )

    def _extract_district(self, norm_text: str) -> Optional[str]:
        # Sắp xếp các alias theo độ dài giảm dần để ưu tiên match cụm dài trước (vd: "tp thu duc" trước "thu duc")
        sorted_aliases = sorted(DISTRICT_ALIASES.keys(), key=len, reverse=True)
        for alias in sorted_aliases:
            pattern = r"(?:\b|^)" + re.escape(alias) + r"(?:\b|$)"
            if re.search(pattern, norm_text):
                return DISTRICT_ALIASES[alias]
        return None

    def _extract_price(self, norm_text: str, raw_text: str) -> Tuple[Optional[float], Optional[float]]:
        # Chuyển đổi các dạng "2 triệu rưỡi" -> "2.5 triệu", "3tr5" -> "3.5tr"
        norm_text = re.sub(r"(\d+)\s*(?:trieu|tr)\s*(?:ruoi|5)\b", r"\1.5 trieu", norm_text)
        norm_text = re.sub(r"(\d+)\s*tr\s*(\d+)\b", r"\1.\2 trieu", norm_text)

        # Dạng khoảng: "từ 2 đến 4 triệu", "2-4tr", "tầm 3 đến 5 củ"
        range_match = re.search(
            r"(?:tu|khoang|tam)?\s*(\d+(?:\.\d+)?)\s*(?:den|toi|-)\s*(\d+(?:\.\d+)?)\s*(?:trieu|tr|cu)\b",
            norm_text
        )
        if range_match:
            min_p = float(range_match.group(1)) * 1_000_000
            max_p = float(range_match.group(2)) * 1_000_000
            return min_p, max_p

        # Dạng khoảng k: "2000k - 3000k"
        range_k = re.search(r"(\d+)\s*k\s*-\s*(\d+)\s*k\b", norm_text)
        if range_k:
            return float(range_k.group(1)) * 1_000, float(range_k.group(2)) * 1_000

        # Dạng giá tối đa (dưới / bé hơn): "dưới 3 triệu", "< 3tr", "tối đa 4 củ"
        max_match = re.search(
            r"(?:duoi|nho hon|toi da|tam|khoang|<|<=)\s*(\d+(?:\.\d+)?)\s*(?:trieu|tr|cu)\b",
            norm_text
        )
        if max_match:
            return None, float(max_match.group(1)) * 1_000_000

        # Dạng giá tối thiểu (trên / lớn hơn): "trên 2 triệu", "> 2tr", "từ 3tr trở lên"
        min_match = re.search(
            r"(?:tren|lon hon|toi thieu|tu|>|>=)\s*(\d+(?:\.\d+)?)\s*(?:trieu|tr|cu)\b",
            norm_text
        )
        if min_match:
            return float(min_match.group(1)) * 1_000_000, None

        # Dạng số triệu đứng sau: "3 triệu", "4tr"
        single_match = re.search(r"(\d+(?:\.\d+)?)\s*(?:trieu|tr|cu)\b", norm_text)
        if single_match:
            return None, float(single_match.group(1)) * 1_000_000

        # Dạng k: "dưới 3500k"
        single_k = re.search(r"(?:duoi|<)?\s*(\d{3,5})\s*k\b", norm_text)
        if single_k:
            return None, float(single_k.group(1)) * 1_000

        return None, None

    def _extract_area(self, norm_text: str) -> Tuple[Optional[float], Optional[float]]:
        # Dạng khoảng: "từ 20 đến 35m2", "20-30m2"
        range_match = re.search(
            r"(?:tu|khoang|tam)?\s*(\d+(?:\.\d+)?)\s*(?:den|toi|-)\s*(\d+(?:\.\d+)?)\s*(?:m2|met vuong)\b",
            norm_text
        )
        if range_match:
            return float(range_match.group(1)), float(range_match.group(2))

        # Dạng diện tích tối thiểu: "trên 25m2", "rộng hơn 20m2"
        min_match = re.search(
            r"(?:tren|lon hon|tu|rong hon|>|>=)\s*(\d+(?:\.\d+)?)\s*(?:m2|met vuong)\b",
            norm_text
        )
        if min_match:
            return float(min_match.group(1)), None

        # Dạng diện tích tối đa: "dưới 30m2"
        max_match = re.search(
            r"(?:duoi|nho hon|toi da|<|<=)\s*(\d+(?:\.\d+)?)\s*(?:m2|met vuong)\b",
            norm_text
        )
        if max_match:
            return None, float(max_match.group(1))

        # Dạng số m2 đơn: "25m2"
        single_match = re.search(r"(\d+(?:\.\d+)?)\s*(?:m2|met vuong)\b", norm_text)
        if single_match:
            val = float(single_match.group(1))
            return val, None

        return None, None

    def _extract_amenities(self, norm_text: str) -> Tuple[List[str], List[int]]:
        detected_names: List[str] = []
        detected_ids: List[int] = []

        for name, am_id, aliases in AMENITY_RULES:
            matched = False
            for alias in aliases:
                pattern = r"(?:\b|^)" + re.escape(alias) + r"(?:\b|$)"
                if re.search(pattern, norm_text):
                    matched = True
                    break
            if matched:
                detected_names.append(name)
                detected_ids.append(am_id)

        return detected_names, detected_ids

    def _extract_keyword(self, norm_text: str) -> Optional[str]:
        for kw_name, aliases in ROOM_TYPE_KEYWORDS:
            for alias in aliases:
                if alias in norm_text:
                    return kw_name
        return None

    def _calculate_confidence(
        self,
        district: Optional[str],
        min_price: Optional[float],
        max_price: Optional[float],
        min_area: Optional[float],
        amenities: List[str],
        keyword: Optional[str]
    ) -> float:
        """
        Tính điểm tin cậy dựa trên số lượng và mức độ rõ ràng của thực thể trích xuất
        """
        score = 0.40  # Mức sàn khi có truy vấn

        if district:
            score += 0.25
        if max_price is not None or min_price is not None:
            score += 0.20
        if amenities:
            score += min(0.15, len(amenities) * 0.08)
        if min_area is not None:
            score += 0.10
        if keyword:
            score += 0.05

        return min(0.98, max(0.25, score))


nlp_service = NlpQueryParserService()
