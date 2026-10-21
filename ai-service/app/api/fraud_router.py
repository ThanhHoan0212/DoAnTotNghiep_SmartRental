import logging
from fastapi import APIRouter, Depends, status
from app.core.security import verify_internal_api_key
from app.schemas.fraud import (
    FraudCheckRequest,
    FraudCheckResponse,
    ListingSyncItem,
    ListingSyncResponse
)
from app.services.fraud_service import fraud_service

logger = logging.getLogger("ai_service.api.fraud")

router = APIRouter(
    prefix="/fraud",
    tags=["Fraud Detection"],
    dependencies=[Depends(verify_internal_api_key)]
)


@router.post(
    "/check-listing",
    response_model=FraudCheckResponse,
    status_code=status.HTTP_200_OK,
    summary="Kiểm tra tin đăng phòng trọ và phát hiện gian lận bằng AI"
)
async def check_listing(request: FraudCheckRequest):
    """
    Endpoint nhận thông tin phòng trọ từ Spring Boot Backend để phân tích:
    1. Phát hiện giá bất thường (Isolation Forest + Thống kê IQR/Z-score theo Quận).
    2. Phát hiện tin đăng trùng lặp mô tả (TF-IDF Cosine Similarity).
    3. Phát hiện từ khóa lừa cọc qua Zalo/Online (NLP Rule-based).
    Trả về điểm rủi ro tổng hợp (fraud_score 0.0 - 1.0) kèm danh sách lý do cụ thể.
    """
    logger.info("Checking fraud for room_id=%s, district=%s, price=%.0f", request.room_id, request.district, request.price)
    return fraud_service.check_listing(request)


@router.post(
    "/sync-listing",
    response_model=ListingSyncResponse,
    status_code=status.HTTP_200_OK,
    summary="Đồng bộ tin đăng mới vào tập dữ liệu đối chiếu trùng lặp"
)
async def sync_listing(item: ListingSyncItem):
    """
    Backend gọi endpoint này khi có tin phòng mới được duyệt để AI cập nhật tập đối chiếu.
    """
    total = fraud_service.sync_listing(item)
    return ListingSyncResponse(
        success=True,
        message=f"Đã đồng bộ phòng #{item.room_id} vào cơ sở dữ liệu đối chiếu trùng lặp.",
        total_listings_in_corpus=total
    )
