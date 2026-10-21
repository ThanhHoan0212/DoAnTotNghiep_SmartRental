import logging
from fastapi import APIRouter, Depends, status
from app.core.security import verify_internal_api_key
from app.schemas.nlp import ParseQueryRequest, ParseQueryResponse
from app.services.nlp_service import nlp_service

logger = logging.getLogger("ai_service.api.nlp")

router = APIRouter(
    prefix="/nlp",
    tags=["NLP Parser"],
    dependencies=[Depends(verify_internal_api_key)]
)


@router.post(
    "/parse-query",
    response_model=ParseQueryResponse,
    status_code=status.HTTP_200_OK,
    summary="Phân tích câu tìm kiếm tự nhiên của người dùng"
)
async def parse_query(request: ParseQueryRequest):
    """
    Endpoint nhận câu tìm kiếm tự nhiên (Natural Language Query) từ Spring Boot Backend:
    - Trích xuất khoảng giá (min_price, max_price), diện tích (min_area, max_area).
    - Trích xuất khu vực (Quận/Huyện chuẩn hóa).
    - Trích xuất danh sách tiện ích và loại phòng.
    - Trả về JSON chứa `filters` và `confidence` giúp Backend dựng câu query PostgreSQL.
    """
    logger.info("Parsing NLP natural query: [%s]", request.query)
    return nlp_service.parse_query(request.query)
