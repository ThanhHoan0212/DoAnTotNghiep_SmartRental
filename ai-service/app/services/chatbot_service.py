import logging
import re
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional, Tuple

from app.schemas.chatbot import (
    ChatbotContext,
    ChatbotRequest,
    ChatbotResponse,
    ChatMessage,
)
from app.services.nlp_service import nlp_service, remove_accents
from app.services.recommendation_service import recommendation_service

logger = logging.getLogger("ai_service.chatbot")


class SessionManager:
    """Quản lý lịch sử hội thoại trong bộ nhớ theo session_id"""
    def __init__(self, max_history_per_session: int = 20):
        self.sessions: Dict[str, List[ChatMessage]] = {}
        self.max_history = max_history_per_session

    def add_message(self, session_id: str, role: str, content: str):
        if session_id not in self.sessions:
            self.sessions[session_id] = []

        now_str = datetime.now(timezone.utc).isoformat()
        msg = ChatMessage(role=role, content=content, timestamp=now_str)
        self.sessions[session_id].append(msg)

        # Giữ số lượng tin nhắn gần nhất để tránh tràn bộ nhớ
        if len(self.sessions[session_id]) > self.max_history:
            self.sessions[session_id] = self.sessions[session_id][-self.max_history:]

    def get_history(self, session_id: str) -> List[ChatMessage]:
        return self.sessions.get(session_id, [])

    def clear_session(self, session_id: str) -> bool:
        if session_id in self.sessions:
            del self.sessions[session_id]
            return True
        return False


INTENT_PATTERNS = [
    ("general_greeting", [
        r"\bxin\s*ch[aà]o\b", r"\bch[aà]o\b", r"\bhello\b", r"\bhi\b", r"\balo\b", r"\bad\s*oi\b"
    ]),
    ("ask_pet_policy", [
        r"th[uú]\s*c[uư]ng", r"nu[oô]i\s*ch[oó]", r"nu[oô]i\s*m[eè]o", r"\bpet\b",
        r"cho\s*nu[oô]i.*kh[oô]ng", r"ch[oó]\s*m[eè]o", r"\bcho meo\b"
    ]),
    ("ask_price_fee", [
        r"gi[aá]\s*(?:thu[eê])?\s*(?:ph[oò]ng)?.*bao\s*nhi[eê]u", r"gi[aá]\s*bao\s*nhi[eê]u",
        r"ti[eề]n\s*di[eệ]n", r"ti[eề]n\s*nu[oớ]c", r"chi\s*ph[ií]", r"c[oọ]c\s*m[aấ]y\s*th[aá]ng",
        r"ti[eề]n\s*c[oọ]c", r"\bbao gia\b", r"\bgia ca\b"
    ]),
    ("ask_contact_contract", [
        r"li[eê]n\s*h[eệ]", r"s[oố]\s*di[eệ]n\s*tho[aạ]i", r"\bsdt\b", r"xem\s*ph[oò]ng",
        r"h[eẹ]n\s*xem", r"h[oợ]p\s*d[oồ]ng", r"k[yý]\s*h[oợ]p\s*d[oồ]ng", r"ch[uủ]\s*nh[aà]"
    ]),
    ("search_new_room", [
        r"t[iì]m\s*ph[oò]ng", r"ki[eế]m\s*ph[oò]ng", r"thu[eê]\s*ph[oò]ng", r"c[oó]\s*ph[oò]ng\s*n[aà]o",
        r"g[oợ]i\s*[yý]\s*ph[oò]ng", r"t[iì]m\s*gi[uú]p", r"d[uư][oớ]i\s*\d+\s*(?:tr|trieu|cu)",
        r"t[uừ]\s*\d+.*(?:tr|trieu|cu)"
    ]),
    ("ask_amenities", [
        r"m[aá]y\s*l[aạ]nh", r"m[aá]y\s*gi[aặ]t", r"g[aá]c\s*l[uử]ng", r"ban\s*c[oô]ng",
        r"t[uủ]\s*l[aạ]nh", r"ti[eệ]n\s*[ií]ch", r"n[oộ]i\s*th[aấ]t", r"n[aấ]u\s*[aă]n",
        r"v[eệ]\s*sinh\s*kh[eé]p\s*k[ií]n", r"khep kin", r"wc rieng"
    ]),
    ("ask_location", [
        r"[oở]\s*d[aâ]u", r"d[iị]a\s*ch[iỉ]", r"v[iị]\s*tr[ií]", r"qu[aậ]n\s*m[aấ]y",
        r"g[aầ]n\s*tr[uư][oờ]ng", r"khu\s*v[uực]\s*n[aà]o", r"h[eẻ]m\s*n[aà]o"
    ])
]


class ChatbotService:
    def __init__(self):
        self.session_manager = SessionManager()

    def classify_intent(self, message: str) -> Tuple[str, float]:
        """Phân loại ý định người dùng (Intent Classification) dựa trên NLP Rules"""
        norm = remove_accents(message.lower())

        for intent, patterns in INTENT_PATTERNS:
            for pat in patterns:
                if re.search(pat, norm):
                    return intent, 0.92

        return "general_fallback", 0.60

    def respond(self, req: ChatbotRequest) -> ChatbotResponse:
        """Xử lý hội thoại thông minh và sinh câu trả lời kèm gợi ý phòng trọ"""
        user_msg = req.message.strip()
        session_id = req.session_id
        ctx = req.context or ChatbotContext()

        # Lưu tin nhắn của user vào session history
        self.session_manager.add_message(session_id=session_id, role="user", content=user_msg)

        # 1. Phân loại ý định
        intent, confidence = self.classify_intent(user_msg)
        suggested_rooms: List[int] = []
        entities: Dict[str, Any] = {}

        # 2. Xử lý phản hồi theo từng intent
        if intent == "ask_pet_policy":
            reply, suggested_rooms = self._handle_pet_policy(ctx)

        elif intent == "ask_price_fee":
            reply = self._handle_price_fee(ctx)

        elif intent == "ask_amenities":
            reply = self._handle_amenities(ctx)

        elif intent == "ask_location":
            reply = self._handle_location(ctx)

        elif intent == "ask_contact_contract":
            reply = self._handle_contact_contract(ctx)

        elif intent == "search_new_room":
            reply, suggested_rooms, entities = self._handle_room_search(user_msg, ctx)

        elif intent == "general_greeting":
            reply = (
                "Xin chào! Tôi là trợ lý ảo SmartRental AI 🤖. "
                "Tôi có thể hỗ trợ bạn tìm phòng theo tiêu chí, kiểm tra tiện ích, "
                "chính sách nuôi thú cưng và thông tin giá thuê. Bạn cần tôi hỗ trợ gì hôm nay?"
            )

        else:  # general_fallback
            reply = (
                "Cảm ơn bạn đã nhắn tin cho SmartRental. Tôi có thể hỗ trợ bạn tìm phòng theo khu vực, "
                "mức giá hoặc tiện ích cụ thể. "
                "Ví dụ: bạn có thể nhập 'tìm phòng dưới 4 triệu ở quận 7 có máy lạnh' nhé!"
            )

        # Lưu câu trả lời của bot vào session history
        self.session_manager.add_message(session_id=session_id, role="assistant", content=reply)

        return ChatbotResponse(
            reply=reply,
            intent=intent,
            suggested_rooms=suggested_rooms,
            session_id=session_id,
            confidence=confidence,
            entities=entities if entities else None
        )

    def _handle_pet_policy(self, ctx: ChatbotContext) -> Tuple[str, List[int]]:
        room_id = ctx.room_id
        suggested: List[int] = []

        if room_id and room_id in recommendation_service.room_metadata_map:
            meta = recommendation_service.room_metadata_map[room_id]
            dist = meta.get("district", "khu vực lân cận")
            ams = meta.get("amenities", [])

            # Kiểm tra xem phòng có tiện ích nuôi thú cưng không
            allows_pet = any("thú cưng" in a.lower() or "pet" in a.lower() for a in ams)
            if allows_pet:
                suggested.append(room_id)
                reply = (
                    f"Phòng #{room_id} tại {dist} CÓ CHÍNH SÁCH CHO PHÉP nuôi thú cưng 🐶🐱. "
                    f"Bạn có thể yên tâm ở cùng boss của mình nhé!"
                )
            else:
                # Tìm các phòng lân cận cùng quận cho phép nuôi thú cưng
                pet_friendly_rooms = [
                    r["room_id"] for r in recommendation_service.room_metadata_map.values()
                    if r.get("district") == dist and any("thú cưng" in a.lower() for a in r.get("amenities", []))
                ][:3]
                suggested.extend(pet_friendly_rooms)

                if pet_friendly_rooms:
                    reply = (
                        f"Phòng #{room_id} hiện tại KHÔNG CHO PHÉP nuôi thú cưng. "
                        f"Tuy nhiên, SmartRental đã tìm giúp bạn các phòng trọ lân cận khu vực {dist} "
                        f"cho phép nuôi pet (phòng #{', #'.join(map(str, pet_friendly_rooms))}). Bạn tham khảo nhé!"
                    )
                else:
                    reply = (
                        f"Rất tiếc, phòng #{room_id} hiện chưa hỗ trợ nuôi thú cưng. "
                        f"Bạn có thể tìm kiếm các phòng khác có bộ lọc 'Cho nuôi thú cưng' trên hệ thống."
                    )
        else:
            reply = (
                "Bạn đang cần tìm phòng trọ cho phép nuôi thú cưng ở khu vực nào và tầm giá bao nhiêu ạ? "
                "Hãy cho tôi biết để tôi lọc ngay các phòng pet-friendly cho bạn nhé!"
            )

        return reply, suggested

    def _handle_price_fee(self, ctx: ChatbotContext) -> str:
        room_id = ctx.room_id
        if room_id and room_id in recommendation_service.room_metadata_map:
            meta = recommendation_service.room_metadata_map[room_id]
            price = meta.get("price", 0)
            area = meta.get("area", 0)
            dist = meta.get("district", "")
            return (
                f"Phòng #{room_id} tại {dist} có giá thuê niêm yết là {int(price):,} đ/tháng cho diện tích {area}m². "
                f"Thông thường tiền đặt cọc tương đương 1 tháng tiền phòng. "
                f"Điện nước tính theo công tơ riêng của phòng. Bạn có thể nhấn 'Đặt lịch xem phòng' để gặp trực tiếp chủ nhà nhé!"
            )
        return (
            "Giá phòng trọ trên SmartRental dao động từ 2.5 triệu đến 10 triệu đồng/tháng tùy theo quận và diện tích. "
            "Bạn đang quan tâm phòng trọ trong khoảng ngân sách bao nhiêu ạ?"
        )

    def _handle_amenities(self, ctx: ChatbotContext) -> str:
        room_id = ctx.room_id
        if room_id and room_id in recommendation_service.room_metadata_map:
            meta = recommendation_service.room_metadata_map[room_id]
            ams = meta.get("amenities", [])
            ams_str = ", ".join(ams) if ams else "đầy đủ tiện nghi cơ bản"
            return (
                f"Phòng #{room_id} ({meta.get('title')}) được trang bị các tiện ích: {ams_str}. "
                f"Phòng khép kín sạch sẽ và giờ giấc tự do."
            )
        return (
            "Hầu hết các phòng trọ trên hệ thống đều có sẵn các tiện ích: Máy lạnh, Máy giặt, Gác lửng, Ban công "
            "và Bảo vệ 24/7. Bạn đang ưu tiên tiện ích nào nhất?"
        )

    def _handle_location(self, ctx: ChatbotContext) -> str:
        room_id = ctx.room_id
        if room_id and room_id in recommendation_service.room_metadata_map:
            meta = recommendation_service.room_metadata_map[room_id]
            dist = meta.get("district", "")
            return (
                f"Phòng #{room_id} tọa lạc tại khu vực {dist}, TP. Hồ Chí Minh. "
                f"Vị trí an ninh, gần các trục đường chính và thuận tiện di chuyển đến các trường đại học/trung tâm."
            )
        return (
            "SmartRental hỗ trợ tìm kiếm phòng trọ tại tất cả các quận huyện TP.HCM (Quận 1, Quận 7, Bình Thạnh, Gò Vấp, Thủ Đức...). "
            "Bạn đang muốn tìm phòng gần trường học hay công ty nào?"
        )

    def _handle_contact_contract(self, ctx: ChatbotContext) -> str:
        room_id = ctx.room_id
        room_str = f"phòng #{room_id}" if room_id else "phòng trọ bạn chọn"
        return (
            f"Để liên hệ chủ nhà hoặc xem {room_str}, bạn có thể nhấn nút 'Liên hệ chủ nhà' "
            f"hoặc 'Đặt lịch hẹn xem phòng' trực tiếp trên giao diện. "
            f"Khi thuê phòng, bạn hoàn toàn có thể ký hợp đồng trực tuyến an toàn bằng chữ ký số qua SmartRental!"
        )

    def _handle_room_search(self, user_msg: str, ctx: ChatbotContext) -> Tuple[str, List[Any], Dict[str, Any]]:
        # Sử dụng NLP service để trích xuất các tiêu chí
        parsed = nlp_service.parse_query(user_msg)
        district = parsed.district or ctx.district
        max_p = parsed.maxPrice
        suggested: List[Any] = []

        # Lấy candidate rooms từ recommendation service
        candidates = recommendation_service._build_candidate_pool(district=district, max_price=max_p)
        suggested = candidates[:4]

        # Bổ sung thông tin chi tiết cho từng phòng đề xuất (title, price, district)
        enriched_rooms = []
        for rid in suggested:
            if rid in recommendation_service.room_metadata_map:
                meta = recommendation_service.room_metadata_map[rid]
                enriched_rooms.append({
                    "room_id": rid,
                    "title": meta.get("title", f"Phòng trọ #{rid}"),
                    "price": meta.get("price", 0),
                    "district": meta.get("district", district or "TP.HCM")
                })
            else:
                enriched_rooms.append({
                    "room_id": rid,
                    "title": f"Phòng trọ #{rid}",
                    "price": 0,
                    "district": district or "TP.HCM"
                })

        dist_str = f"khu vực {district}" if district else "TP.HCM"
        price_str = f"dưới {int(max_p):,}đ" if max_p else "mức giá tốt nhất"

        if enriched_rooms:
            titles = [f"\"{r['title']}\"" for r in enriched_rooms[:2]]
            titles_str = f": {', '.join(titles)}" if titles else ""
            reply = (
                f"Tôi đã tìm thấy phòng trọ phù hợp với tiêu chí của bạn tại {dist_str} ({price_str}){titles_str}. "
                f"Bạn có thể bấm vào thẻ phòng bên dưới để xem ảnh thực tế, tiện ích và đặt lịch xem nhé!"
            )
        else:
            # Fallback phòng nổi bật
            popular = [r["room_id"] for r in recommendation_service.popular_rooms[:3]]
            for rid in popular:
                meta = recommendation_service.room_metadata_map.get(rid, {})
                enriched_rooms.append({
                    "room_id": rid,
                    "title": meta.get("title", f"Phòng trọ #{rid}"),
                    "price": meta.get("price", 0),
                    "district": meta.get("district", "TP.HCM")
                })
            reply = (
                f"Hiện tại khu vực {dist_str} chưa có phòng trống khớp chính xác tiêu chí. "
                f"Dưới đây là các phòng trọ nổi bật nhất trên hệ thống mà bạn có thể tham khảo:"
            )

        entities_dict = {
            "district": district,
            "max_price": max_p,
            "min_price": parsed.minPrice,
            "amenities": parsed.detectedAmenities
        }

        return reply, enriched_rooms, entities_dict



chatbot_service = ChatbotService()
