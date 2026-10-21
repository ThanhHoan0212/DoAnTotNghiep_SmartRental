import logging
from fastapi import APIRouter, Depends, status
from app.core.security import verify_internal_api_key
from app.schemas.chatbot import (
    ChatHistoryResponse,
    ChatbotRequest,
    ChatbotResponse,
)
from app.schemas.common import BaseResponse
from app.services.chatbot_service import chatbot_service

logger = logging.getLogger("ai_service.api.chatbot")

router = APIRouter(
    prefix="/chatbot",
    tags=["Chatbot Assistant"],
    dependencies=[Depends(verify_internal_api_key)]
)


@router.post(
    "/respond",
    response_model=ChatbotResponse,
    status_code=status.HTTP_200_OK,
    summary="Tương tác với Chatbot tư vấn phòng trọ AI"
)
async def chat_respond(request: ChatbotRequest):
    """
    Endpoint nhận tin nhắn hội thoại từ người dùng:
    - Phân loại ý định (ask_pet_policy, ask_price_fee, ask_amenities, search_new_room, ...).
    - Duy trì lịch sử theo session_id.
    - Trả về câu trả lời tự nhiên (`reply`) và danh sách phòng gợi ý (`suggested_rooms`).
    """
    logger.info("Chatbot message from session=[%s]: '%s'", request.session_id, request.message)
    return chatbot_service.respond(request)


@router.get(
    "/history/{session_id}",
    response_model=ChatHistoryResponse,
    status_code=status.HTTP_200_OK,
    summary="Lấy lịch sử hội thoại của một session"
)
async def get_chat_history(session_id: str):
    """Lấy danh sách các tin nhắn trong phiên hội thoại hiện tại"""
    history = chatbot_service.session_manager.get_history(session_id)
    return ChatHistoryResponse(
        session_id=session_id,
        total_messages=len(history),
        messages=history
    )


@router.delete(
    "/history/{session_id}",
    response_model=BaseResponse,
    status_code=status.HTTP_200_OK,
    summary="Xóa phiên hội thoại"
)
async def clear_chat_history(session_id: str):
    """Xóa bỏ lịch sử phiên hội thoại khi người dùng kết thúc hoặc bắt đầu phiên mới"""
    cleared = chatbot_service.session_manager.clear_session(session_id)
    return BaseResponse(
        success=True,
        message=f"Đã xóa phiên hội thoại {session_id}." if cleared else "Phiên không tồn tại hoặc đã được xóa."
    )
