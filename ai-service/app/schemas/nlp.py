from typing import Any, Dict, List, Optional
from pydantic import BaseModel, Field


class ParseQueryRequest(BaseModel):
    query: str = Field(
        ...,
        min_length=2,
        examples=["tìm phòng dưới 3 triệu gần quận 1 có chỗ để xe, ban công"],
        description="Câu truy vấn tìm kiếm tự nhiên của người dùng bằng tiếng Việt"
    )


class ParsedFilters(BaseModel):
    max_price: Optional[float] = Field(default=None, examples=[3000000.0])
    min_price: Optional[float] = Field(default=None, examples=[None])
    district: Optional[str] = Field(default=None, examples=["Quận 1"])
    amenities: List[str] = Field(default=[], examples=[["Chỗ để xe", "Ban công"]])
    min_area: Optional[float] = Field(default=None, examples=[None])
    max_area: Optional[float] = Field(default=None, examples=[None])


class ParseQueryResponse(BaseModel):
    success: bool = Field(default=True, examples=[True])
    originalQuery: str = Field(
        ...,
        examples=["tìm phòng dưới 3 triệu gần quận 1 có chỗ để xe, ban công"],
        description="Câu truy vấn gốc"
    )
    keyword: Optional[str] = Field(default=None, examples=["phòng trọ"])
    district: Optional[str] = Field(default=None, examples=["Quận 1"])
    minPrice: Optional[float] = Field(default=None, examples=[None])
    maxPrice: Optional[float] = Field(default=None, examples=[3000000.0])
    minArea: Optional[float] = Field(default=None, examples=[None])
    maxArea: Optional[float] = Field(default=None, examples=[None])
    detectedAmenities: List[str] = Field(
        default=[],
        examples=[["Chỗ để xe", "Ban công"]]
    )
    amenityIds: List[int] = Field(default=[], examples=[[1, 5]])
    confidence: float = Field(
        ...,
        ge=0.0,
        le=1.0,
        examples=[0.87],
        description="Độ tin cậy của kết quả phân tích NLP (0.0 - 1.0)"
    )
    source: str = Field(default="AI_SERVICE", examples=["AI_SERVICE"])
    filters: ParsedFilters
