import pytest
from httpx import ASGITransport, AsyncClient
from app.main import app
from app.core.config import settings


@pytest.fixture
def auth_headers():
    return {"X-Internal-Api-Key": settings.INTERNAL_API_KEY}


@pytest.mark.asyncio
async def test_price_prediction_unauthorized():
    """Kiểm tra chặn truy cập dự đoán giá nếu không có API key nội bộ"""
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.get("/ai/price-prediction?district=Quận 7")
    assert response.status_code == 401
    assert response.json()["error_code"] == "MISSING_API_KEY"


@pytest.mark.asyncio
async def test_price_prediction_prompt_sample(auth_headers):
    """Kiểm tra endpoint mẫu trong prompt:
    GET /ai/price-prediction?district=Quận 7
    """
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.get("/ai/price-prediction?district=Quận 7", headers=auth_headers)

    assert response.status_code == 200
    data = response.json()
    assert data["district"] == "Quận 7"
    assert data["current_avg_price"] > 0
    assert data["predicted_next_month"] > 0
    assert data["trend"] in ["increasing", "decreasing", "stable"]
    assert len(data["confidence_interval"]) == 2
    assert data["confidence_interval"][0] <= data["confidence_interval"][1]
    assert isinstance(data["factors"], list)
    assert len(data["factors"]) > 0


@pytest.mark.asyncio
async def test_price_prediction_with_alias_and_room_type(auth_headers):
    """Kiểm tra chuẩn hóa alias quận (q1 -> Quận 1) và phân khúc studio"""
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.get("/ai/price-prediction?district=q1&room_type=studio", headers=auth_headers)

    assert response.status_code == 200
    data = response.json()
    assert data["district"] == "Quận 1"
    assert data["room_type"] == "studio"
    # Giá studio Quận 1 trung bình cao (> 7 triệu)
    assert data["current_avg_price"] >= 7000000.0


@pytest.mark.asyncio
async def test_price_market_overview(auth_headers):
    """Kiểm tra endpoint tổng quan thị trường toàn TP.HCM"""
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.get("/ai/price-prediction/market-overview", headers=auth_headers)

    assert response.status_code == 200
    data = response.json()
    assert "updated_at" in data
    assert data["overall_market_trend"] in ["increasing", "decreasing", "stable"]
    assert len(data["districts"]) >= 10
    first = data["districts"][0]
    assert "district" in first
    assert "avg_price" in first
    assert "trend" in first
