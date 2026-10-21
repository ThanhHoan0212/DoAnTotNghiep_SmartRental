from enum import Enum
from typing import Any, Dict, List, Optional
from pydantic import BaseModel, Field


class RiskLevel(str, Enum):
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"


class FraudCheckRequest(BaseModel):
    room_id: Optional[int] = Field(
        default=None,
        examples=[999],
        description="ID phòng trọ từ Backend (nếu có)"
    )
    title: str = Field(
        ...,
        min_length=5,
        examples=["Cho thuê phòng trọ cao cấp full nội thất"],
        description="Tiêu đề tin đăng phòng"
    )
    description: str = Field(
        ...,
        min_length=10,
        examples=["Phòng mới xây, đầy đủ máy lạnh, tủ lạnh, giường nệm. Giờ giấc tự do, có bảo vệ 24/7."],
        description="Nội dung mô tả chi tiết phòng trọ"
    )
    price: float = Field(
        ...,
        gt=0,
        examples=[1500000.0],
        description="Giá thuê phòng mỗi tháng (VND)"
    )
    district: str = Field(
        ...,
        examples=["Quận 7"],
        description="Quận/Huyện nơi có phòng trọ"
    )
    area: float = Field(
        ...,
        gt=0,
        examples=[25.0],
        description="Diện tích phòng (m²)"
    )
    image_urls: Optional[List[str]] = Field(
        default=[],
        examples=[["https://example.com/room1.jpg", "https://example.com/room2.jpg"]],
        description="Danh sách đường dẫn ảnh tin đăng"
    )


class FraudCheckResponse(BaseModel):
    room_id: Optional[int] = Field(default=None, examples=[999])
    fraud_score: float = Field(
        ...,
        ge=0.0,
        le=1.0,
        examples=[0.73],
        description="Điểm rủi ro gian lận tổng hợp (0.0 đến 1.0)"
    )
    risk_level: RiskLevel = Field(
        ...,
        examples=[RiskLevel.HIGH],
        description="Mức độ rủi ro: LOW, MEDIUM, HIGH"
    )
    is_fraud: bool = Field(
        ...,
        examples=[True],
        description="Nhận định có dấu hiệu gian lận hay không"
    )
    reasons: List[str] = Field(
        default=[],
        examples=[[
            "Giá thấp bất thường so với khu vực Quận 7 (trung bình 3.800.000đ)",
            "Nội dung mô tả trùng >90% với tin đăng #456"
        ]],
        description="Danh sách lý do cụ thể sinh ra từ các mô hình AI"
    )
    details: Optional[Dict[str, Any]] = Field(
        default=None,
        description="Chi tiết các điểm thành phần (giá, trùng lặp, từ khóa) phục vụ giải trình hội đồng"
    )


class ListingSyncItem(BaseModel):
    room_id: int = Field(..., examples=[101])
    title: str = Field(..., examples=["Phòng trọ ban công Quận 10"])
    description: str = Field(..., examples=["Phòng rộng rãi, máy lạnh, máy giặt, kệ bếp riêng..."])
    price: float = Field(..., examples=[4500000.0])
    district: str = Field(..., examples=["Quận 10"])
    area: float = Field(..., examples=[28.0])


class ListingSyncResponse(BaseModel):
    success: bool = True
    message: str
    total_listings_in_corpus: int
