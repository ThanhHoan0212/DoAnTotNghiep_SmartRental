from enum import Enum
from typing import List, Optional
from pydantic import BaseModel, Field


class TrendDirection(str, Enum):
    INCREASING = "increasing"
    DECREASING = "decreasing"
    STABLE = "stable"


class PricePredictionResponse(BaseModel):
    district: str = Field(..., examples=["Quận 7"], description="Quận/Huyện dự đoán")
    room_type: Optional[str] = Field(default=None, examples=["phòng trọ"], description="Loại phòng (nếu có)")
    current_avg_price: float = Field(..., examples=[3200000.0], description="Giá trung bình hiện tại (VND)")
    predicted_next_month: float = Field(..., examples=[3350000.0], description="Giá dự đoán tháng tiếp theo (VND)")
    trend: TrendDirection = Field(..., examples=[TrendDirection.INCREASING], description="Xu hướng: increasing, decreasing, stable")
    confidence_interval: List[float] = Field(
        ...,
        examples=[[3100000.0, 3600000.0]],
        description="Khoảng tin cậy 95% [giá_tối_thiểu, giá_tối_đa]"
    )
    change_percentage: Optional[float] = Field(default=None, examples=[4.69], description="Tỷ lệ biến động giá dự kiến (%)")
    factors: List[str] = Field(
        default=[],
        examples=[["Mùa sinh viên nhập học đẩy nhu cầu thuê tăng cao", "Nhu cầu căn hộ mini khu vực Quận 7 tăng 5%"]],
        description="Các yếu tố thị trường ảnh hưởng đến xu hướng giá"
    )


class DistrictPriceOverview(BaseModel):
    district: str = Field(..., examples=["Quận 7"])
    avg_price: float = Field(..., examples=[4350000.0])
    min_price: float = Field(..., examples=[2200000.0])
    max_price: float = Field(..., examples=[8500000.0])
    avg_price_per_m2: float = Field(..., examples=[195000.0])
    trend: TrendDirection = Field(..., examples=[TrendDirection.INCREASING])
    sample_count: int = Field(..., examples=[120])


class MarketTrendSummaryResponse(BaseModel):
    updated_at: str
    overall_market_trend: TrendDirection
    districts: List[DistrictPriceOverview]
