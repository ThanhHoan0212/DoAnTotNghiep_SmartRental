import pytest
from httpx import ASGITransport, AsyncClient
from app.main import app
from app.core.config import settings


@pytest.fixture
def auth_headers():
    return {"X-Internal-Api-Key": settings.INTERNAL_API_KEY}


@pytest.mark.asyncio
async def test_chatbot_unauthorized():
    """Kiểm tra chặn chatbot nếu không có API key nội bộ"""
    payload = {
        "session_id": "test-session",
        "message": "xin chào"
    }
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.post("/ai/chatbot/respond", json=payload)
    assert response.status_code == 401
    assert response.json()["error_code"] == "MISSING_API_KEY"


@pytest.mark.asyncio
async def test_chatbot_pet_policy_prompt_sample(auth_headers):
    """Kiểm tra câu hỏi mẫu trong prompt:
    'phòng này có được nuôi thú cưng không?' với context room_id=45
    """
    payload = {
        "user_id": 123,
        "session_id": "abc-xyz",
        "message": "phòng này có được nuôi thú cưng không?",
        "context": {"room_id": 45}
    }
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.post("/ai/chatbot/respond", json=payload, headers=auth_headers)

    assert response.status_code == 200
    data = response.json()
    assert data["intent"] == "ask_pet_policy"
    assert isinstance(data["reply"], str)
    assert len(data["reply"]) > 10
    assert isinstance(data["suggested_rooms"], list)
    assert data["session_id"] == "abc-xyz"


@pytest.mark.asyncio
async def test_chatbot_price_fee(auth_headers):
    """Kiểm tra hỏi giá và chi phí"""
    payload = {
        "session_id": "session-price",
        "message": "giá thuê phòng này bao nhiêu vậy bạn?",
        "context": {"room_id": 45}
    }
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.post("/ai/chatbot/respond", json=payload, headers=auth_headers)

    assert response.status_code == 200
    data = response.json()
    assert data["intent"] == "ask_price_fee"
    assert any(term in data["reply"].lower() for term in ["giá", "tháng", "cọc"])


@pytest.mark.asyncio
async def test_chatbot_search_new_room(auth_headers):
    """Kiểm tra chatbot tìm phòng mới trả về suggested_rooms"""
    payload = {
        "session_id": "session-search",
        "message": "tìm phòng dưới 4 triệu ở quận 7 có ban công",
        "context": None
    }
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.post("/ai/chatbot/respond", json=payload, headers=auth_headers)

    assert response.status_code == 200
    data = response.json()
    assert data["intent"] == "search_new_room"
    assert len(data["suggested_rooms"]) > 0


@pytest.mark.asyncio
async def test_chatbot_session_history(auth_headers):
    """Kiểm tra lưu trữ và quản lý lịch sử hội thoại nhiều lượt (Multi-turn History)"""
    session_id = "session-multi-turn-test"
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        # Lượt 1
        res1 = await ac.post("/ai/chatbot/respond", json={
            "session_id": session_id,
            "message": "xin chào trợ lý"
        }, headers=auth_headers)
        assert res1.status_code == 200

        # Lượt 2
        res2 = await ac.post("/ai/chatbot/respond", json={
            "session_id": session_id,
            "message": "cho mình hỏi về phòng trọ"
        }, headers=auth_headers)
        assert res2.status_code == 200

        # Lấy lịch sử hội thoại
        history_res = await ac.get(f"/ai/chatbot/history/{session_id}", headers=auth_headers)
        assert history_res.status_code == 200
        history_data = history_res.json()
        assert history_data["total_messages"] == 4  # 2 user + 2 assistant
        assert history_data["messages"][0]["role"] == "user"
        assert history_data["messages"][1]["role"] == "assistant"

        # Xóa phiên
        del_res = await ac.delete(f"/ai/chatbot/history/{session_id}", headers=auth_headers)
        assert del_res.status_code == 200
        assert del_res.json()["success"] is True
