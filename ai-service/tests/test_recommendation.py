import pytest
from httpx import ASGITransport, AsyncClient
from app.main import app
from app.core.config import settings


@pytest.fixture
def auth_headers():
    return {"X-Internal-Api-Key": settings.INTERNAL_API_KEY}


@pytest.mark.asyncio
async def test_recommendation_unauthorized():
    """Kiểm tra chặn truy cập gợi ý nếu không có API key nội bộ"""
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.get("/ai/recommendation/1")
    assert response.status_code == 401
    assert response.json()["error_code"] == "MISSING_API_KEY"


@pytest.mark.asyncio
async def test_recommendation_cold_start(auth_headers):
    """Kiểm tra gợi ý cho người dùng mới (Cold-Start) -> trả về phòng nổi bật phổ biến"""
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        # User ID 99999 chưa từng có lịch sử tương tác
        response = await ac.get("/ai/recommendation/99999?limit=5", headers=auth_headers)

    assert response.status_code == 200
    data = response.json()
    assert data["user_id"] == 99999
    assert data["strategy"] == "cold_start_popular"
    assert len(data["recommended_room_ids"]) == 5
    assert len(data["items"]) == 5
    assert all("Phòng trọ nổi bật" in item["reason"] for item in data["items"])


@pytest.mark.asyncio
async def test_recommendation_existing_user(auth_headers):
    """Kiểm tra gợi ý cho người dùng đã có lịch sử -> áp dụng Content-Based Filtering"""
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        # User ID 1 có trong tập dữ liệu synthetic
        response = await ac.get("/ai/recommendation/1?limit=5", headers=auth_headers)

    assert response.status_code == 200
    data = response.json()
    assert data["user_id"] == 1
    assert data["strategy"] == "content_based"
    assert len(data["recommended_room_ids"]) == 5
    assert len(data["items"]) == 5
    # Kiểm tra các điểm tương đồng cosine hợp lệ
    for item in data["items"]:
        assert 0.0 <= item["score"] <= 1.0
        assert item["reason"] is not None


@pytest.mark.asyncio
async def test_personalized_recommendation_with_favorites(auth_headers):
    """Kiểm tra gợi ý thời gian thực khi truyền danh sách Favorite Rooms trong body"""
    payload = {
        "user_id": 888,
        "limit": 4,
        "favorite_room_ids": [1, 2],
        "preferred_district": "Quận 10",
        "preferred_max_price": 5000000.0
    }
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.post("/ai/recommendation/personalized", json=payload, headers=auth_headers)

    assert response.status_code == 200
    data = response.json()
    assert data["user_id"] == 888
    assert data["strategy"] == "content_based"
    assert len(data["recommended_room_ids"]) == 4
    # Không gợi ý lại các phòng user đã đưa vào danh sách yêu thích
    assert 1 not in data["recommended_room_ids"]
    assert 2 not in data["recommended_room_ids"]


@pytest.mark.asyncio
async def test_sync_interaction_and_room(auth_headers):
    """Kiểm tra các endpoint đồng bộ tương tác và phòng mới"""
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        # 1. Đồng bộ tương tác mới
        interaction_payload = {
            "user_id": 555,
            "room_id": 10,
            "interaction_type": "FAVORITE"
        }
        res1 = await ac.post("/ai/recommendation/sync-interaction", json=interaction_payload, headers=auth_headers)
        assert res1.status_code == 200
        assert res1.json()["success"] is True

        # 2. Đồng bộ phòng mới
        room_payload = {
            "room_id": 9999,
            "title": "Phòng Studio cao cấp Vinhomes Grand Park",
            "district": "Thủ Đức",
            "price": 6000000.0,
            "area": 30.0,
            "amenities": ["Máy lạnh", "Ban công", "Bảo vệ 24/7"]
        }
        res2 = await ac.post("/ai/recommendation/sync-room", json=room_payload, headers=auth_headers)
        assert res2.status_code == 200
        assert res2.json()["success"] is True
