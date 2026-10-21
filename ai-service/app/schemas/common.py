from datetime import datetime, timezone
from typing import Any, Optional
from pydantic import BaseModel, Field


class HealthCheckResponse(BaseModel):
    status: str = Field(default="ok", examples=["ok"])
    service: str = Field(default="SmartRental AI Service", examples=["SmartRental AI Service"])
    version: str = Field(default="1.0.0", examples=["1.0.0"])
    environment: str = Field(default="development", examples=["development"])
    timestamp: str = Field(
        default_factory=lambda: datetime.now(timezone.utc).isoformat(),
        examples=["2026-10-18T09:00:00Z"]
    )


class StandardErrorResponse(BaseModel):
    success: bool = Field(default=False, examples=[False])
    error_code: str = Field(examples=["MODEL_NOT_READY"])
    message: str = Field(examples=["Mô hình AI chưa sẵn sàng hoặc đang khởi tạo."])
    detail: Optional[Any] = None


class BaseResponse(BaseModel):
    success: bool = Field(default=True, examples=[True])
    message: Optional[str] = Field(default=None, examples=["Thành công"])
