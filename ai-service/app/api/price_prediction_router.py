import logging
from typing import Optional
from fastapi import APIRouter, Depends, Query, status
from app.core.security import verify_internal_api_key
from app.schemas.price import (
    MarketTrendSummaryResponse,
    PricePredictionResponse,
)
from app.services.price_service import price_service

logger = logging.getLogger("ai_service.api.price")

router = APIRouter(
    prefix="/price-prediction",
    tags=["Price Prediction & Trends"],
    dependencies=[Depends(verify_internal_api_key)]
)


@router.get(
    "",
    response_model=PricePredictionResponse,
    status_code=status.HTTP_200_OK,
    summary="Dự đoán xu hướng giá thuê phòng trọ theo Quận và Loại phòng"
)
async def predict_price(
    district: str = Query(..., examples=["Quận 7"], description="Tên quận/huyện cần dự đoán"),
    room_type: Optional[str] = Query(default=None, examples=["phòng trọ"], description="Loại phòng (phòng trọ, chung cư mini, studio)")
):
    """
    Endpoint dự đoán giá thuê trung bình tháng tới và phân tích xu hướng:
    - Thuật toán: Hồi quy Gradient Boosting kết hợp chuỗi thời gian & tính mùa vụ.
    - Trả về: current_avg_price, predicted_next_month, trend, confidence_interval 95% và factors.
    """
    logger.info("Price prediction requested for district=%s, room_type=%s", district, room_type)
    return price_service.predict_price_trend(district=district, room_type=room_type)


@router.get(
    "/market-overview",
    response_model=MarketTrendSummaryResponse,
    status_code=status.HTTP_200_OK,
    summary="Tổng quan thị trường và xu hướng giá toàn TP.HCM"
)
async def get_market_overview():
    """Lấy dữ liệu tổng quan giá thuê trung bình và xu hướng biến động trên toàn bộ các quận TP.HCM"""
    return price_service.get_market_overview()
