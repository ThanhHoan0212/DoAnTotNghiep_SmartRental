import logging
from typing import Optional
from fastapi import APIRouter, Depends, Query, status
from app.core.security import verify_internal_api_key
from app.schemas.common import BaseResponse
from app.schemas.recommendation import (
    PersonalizedRecommendRequest,
    RecommendationResponse,
    RoomFeatureItem,
    UserInteractionSync,
)
from app.services.recommendation_service import recommendation_service

logger = logging.getLogger("ai_service.api.recommendation")

router = APIRouter(
    prefix="/recommendation",
    tags=["Recommendation System"],
    dependencies=[Depends(verify_internal_api_key)]
)


@router.get(
    "/{user_id}",
    response_model=RecommendationResponse,
    status_code=status.HTTP_200_OK,
    summary="Gợi ý phòng trọ cá nhân hóa cho người dùng"
)
async def get_recommendation(
    user_id: str,
    limit: int = Query(default=10, ge=1, le=50, description="Số lượng phòng gợi ý"),
    district: Optional[str] = Query(default=None, description="Lọc theo quận nếu có")
):
    """
    Endpoint trả về danh sách ID phòng trọ phù hợp nhất cho user_id:
    - Nếu user đã có lịch sử tìm kiếm/yêu thích: Áp dụng Content-Based Filtering.
    - Nếu user mới (Cold-Start): Trả về danh sách phòng nổi bật/phổ biến nhất.
    """
    parsed_user_id = int(user_id) if user_id.isdigit() else user_id
    logger.info("Recommendation requested for user_id=%s, limit=%d, district=%s", parsed_user_id, limit, district)
    return recommendation_service.get_recommendations(user_id=parsed_user_id, limit=limit, district=district)



@router.post(
    "/sync-all-rooms",
    response_model=BaseResponse,
    status_code=status.HTTP_200_OK,
    summary="Đồng bộ toàn bộ danh mục phòng trọ từ PostgreSQL qua Backend API"
)
async def sync_all_rooms():
    """
    Kích hoạt tiến trình quét toàn bộ phòng trọ từ database PostgreSQL qua Spring Boot,
    chuẩn hóa dữ liệu, tái tạo ma trận đặc trưng và nạp lại vào bộ nhớ.
    """
    count = recommendation_service.sync_all_from_database()
    return BaseResponse(
        success=True,
        message=f"Đã đồng bộ thành công {count} phòng trọ từ PostgreSQL vào hệ thống gợi ý AI."
    )



@router.post(
    "/personalized",
    response_model=RecommendationResponse,
    status_code=status.HTTP_200_OK,
    summary="Gợi ý phòng trọ theo danh sách phòng yêu thích/đã xem tức thời"
)
async def get_personalized_recommendation(req: PersonalizedRecommendRequest):
    """
    Cho phép Spring Boot Backend truyền trực tiếp danh sách Favorite / Viewed rooms
    của user trong session hiện tại để nhận gợi ý tức thì mà không cần đồng bộ DB trước.
    """
    logger.info("Personalized recommendation for user_id=%d with %d favorites, %d views",
                req.user_id, len(req.favorite_room_ids or []), len(req.viewed_room_ids or []))
    return recommendation_service.get_personalized_recommendations(req)


@router.post(
    "/sync-interaction",
    response_model=BaseResponse,
    status_code=status.HTTP_200_OK,
    summary="Đồng bộ hành vi tương tác của người dùng (Xem, Thích, Liên hệ)"
)
async def sync_interaction(interaction: UserInteractionSync):
    """
    Backend gọi endpoint này khi người dùng thực hiện Xem (VIEW) hoặc Lưu yêu thích (FAVORITE)
    để AI Service cập nhật hồ sơ sở thích theo thời gian thực (Real-time Profile Update).
    """
    recommendation_service.record_interaction(interaction)
    return BaseResponse(
        success=True,
        message=f"Đã cập nhật tương tác {interaction.interaction_type} của user #{interaction.user_id}."
    )


@router.post(
    "/sync-room",
    response_model=BaseResponse,
    status_code=status.HTTP_200_OK,
    summary="Đồng bộ thông tin phòng trọ mới vào không gian vector gợi ý"
)
async def sync_room(room: RoomFeatureItem):
    """
    Backend gọi endpoint này khi có phòng trọ mới được duyệt để AI vector hóa và đưa vào tập ứng viên.
    """
    recommendation_service.sync_room(room)
    return BaseResponse(
        success=True,
        message=f"Đã vector hóa và lưu phòng #{room.room_id} ('{room.title}') vào hệ thống gợi ý."
    )
