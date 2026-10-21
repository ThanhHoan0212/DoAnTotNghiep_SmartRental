from typing import Any, Dict, List, Optional
from pydantic import BaseModel, Field


class ChatbotContext(BaseModel):
    room_id: Optional[int] = Field(default=None, examples=[45], description="ID phòng trọ đang xem")
    district: Optional[str] = Field(default=None, examples=["Quận 7"], description="Khu vực đang xem")
    current_page: Optional[str] = Field(default=None, examples=["room_detail"], description="Trang hiện tại của user")


class ChatbotRequest(BaseModel):
    user_id: Optional[int] = Field(default=None, examples=[123])
    session_id: str = Field(..., min_length=1, examples=["session-abc-123"], description="ID phiên hội thoại")
    message: str = Field(..., min_length=1, examples=["phòng này có được nuôi thú cưng không?"])
    context: Optional[ChatbotContext] = None


class ChatbotResponse(BaseModel):
    reply: str = Field(
        ...,
        examples=["Phòng #45 hiện tại cho phép nuôi thú cưng. Bạn có thể liên hệ chủ nhà để trao đổi cụ thể hơn nhé!"],
        description="Câu trả lời tự nhiên của trợ lý AI"
    )
    intent: str = Field(
        ...,
        examples=["ask_pet_policy"],
        description="Ý định người dùng được nhận diện: ask_pet_policy, ask_price_fee, ask_amenities, search_new_room, ask_location, ask_contact_contract, general_greeting"
    )
    suggested_rooms: List[Any] = Field(
        default=[],
        examples=[[45, 78]],
        description="Danh sách ID hoặc đối tượng phòng trọ đề xuất liên quan"
    )
    session_id: str = Field(..., examples=["session-abc-123"])
    confidence: float = Field(default=0.90, ge=0.0, le=1.0, examples=[0.92])
    entities: Optional[Dict[str, Any]] = Field(default=None, description="Các thực thể trích xuất nếu có")


class ChatMessage(BaseModel):
    role: str = Field(..., examples=["user"])
    content: str = Field(..., examples=["phòng này có được nuôi thú cưng không?"])
    timestamp: str


class ChatHistoryResponse(BaseModel):
    session_id: str
    total_messages: int
    messages: List[ChatMessage]
