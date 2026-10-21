from typing import Any, Dict, List, Optional, Union
from pydantic import BaseModel, Field


class RecommendationItem(BaseModel):
    room_id: Union[str, int] = Field(..., examples=["b0000000-0000-0000-0000-000000000001", 45])
    score: float = Field(..., ge=0.0, le=1.0, examples=[0.92], description="Độ tương đồng Cosine (0.0 - 1.0)")
    title: Optional[str] = Field(default=None, examples=["Phòng trọ cao cấp Bình Thạnh"])
    district: Optional[str] = Field(default=None, examples=["Bình Thạnh"])
    price: Optional[float] = Field(default=None, examples=[3800000.0])
    area: Optional[float] = Field(default=None, examples=[28.0])
    reason: Optional[str] = Field(
        default=None,
        examples=["Phù hợp với sở thích về khu vực Bình Thạnh và tầm giá dưới 4 triệu của bạn"]
    )


class RecommendationResponse(BaseModel):
    user_id: Union[str, int] = Field(..., examples=[123, "a0000000-0000-0000-0000-000000000001"])
    recommended_room_ids: List[Union[str, int]] = Field(
        ...,
        examples=[["b0000000-0000-0000-0000-000000000001", 12]],
        description="Danh sách ID phòng trọ gợi ý sắp xếp theo độ phù hợp giảm dần"
    )
    strategy: str = Field(
        ...,
        examples=["content_based"],
        description="Chiến lược gợi ý: 'content_based' (dựa trên lịch sử) hoặc 'cold_start_popular' (người dùng mới)"
    )
    items: Optional[List[RecommendationItem]] = Field(
        default=[],
        description="Chi tiết từng phòng gợi ý kèm điểm số và lý do đề xuất"
    )
    recommendations: Optional[List[RecommendationItem]] = Field(
        default=[],
        description="Alias cho items để tương thích với Backend DTO"
    )


class UserInteractionSync(BaseModel):
    user_id: Union[str, int] = Field(..., examples=[123, "a0000000-0000-0000-0000-000000000001"])
    room_id: Union[str, int] = Field(..., examples=["b0000000-0000-0000-0000-000000000001", 45])
    interaction_type: str = Field(
        default="VIEW",
        examples=["FAVORITE"],
        description="Loại tương tác: 'VIEW', 'FAVORITE', 'CONTACT', 'SEARCH'"
    )


class PersonalizedRecommendRequest(BaseModel):
    user_id: Union[str, int] = Field(..., examples=[123, "a0000000-0000-0000-0000-000000000001"])
    limit: int = Field(default=10, ge=1, le=50, examples=[10])
    favorite_room_ids: Optional[List[Union[str, int]]] = Field(default=[], examples=[["b0000000-0000-0000-0000-000000000001", 15]])
    viewed_room_ids: Optional[List[Union[str, int]]] = Field(default=[], examples=[["b0000000-0000-0000-0000-000000000002", 25]])
    preferred_district: Optional[str] = Field(default=None, examples=["Bình Thạnh"])
    preferred_max_price: Optional[float] = Field(default=None, examples=[5000000.0])


class RoomFeatureItem(BaseModel):
    room_id: Union[str, int] = Field(..., examples=["b0000000-0000-0000-0000-000000000001", 45])
    title: str = Field(..., examples=["Phòng trọ cao cấp Bình Thạnh"])
    district: str = Field(..., examples=["Bình Thạnh"])
    price: float = Field(..., gt=0, examples=[3800000.0])
    area: float = Field(..., gt=0, examples=[28.0])
    amenities: List[str] = Field(
        default=[],
        examples=[["Điều hòa nhiệt độ", "Bình nóng lạnh", "Wifi tốc độ cao", "Chỗ để xe miễn phí"]]
    )

