import pytest
from httpx import ASGITransport, AsyncClient
from app.main import app
from app.core.config import settings


@pytest.fixture
def auth_headers():
    return {"X-Internal-Api-Key": settings.INTERNAL_API_KEY}


@pytest.mark.asyncio
async def test_nlp_parse_unauthorized():
    """Kiểm tra chặn phân tích NLP nếu thiếu API key nội bộ"""
    payload = {"query": "tìm phòng trọ quận 1"}
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.post("/ai/nlp/parse-query", json=payload)
    assert response.status_code == 401
    assert response.json()["error_code"] == "MISSING_API_KEY"


@pytest.mark.asyncio
async def test_nlp_parse_prompt_sample_query(auth_headers):
    """Kiểm tra đúng câu query mẫu trong prompt:
    'tìm phòng dưới 3 triệu gần quận 1 có chỗ để xe, ban công'
    """
    payload = {"query": "tìm phòng dưới 3 triệu gần quận 1 có chỗ để xe, ban công"}
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.post("/ai/nlp/parse-query", json=payload, headers=auth_headers)

    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True
    assert data["district"] == "Quận 1"
    assert data["maxPrice"] == 3000000.0
    assert data["minPrice"] is None
    assert "chỗ để xe" in data["detectedAmenities"]
    assert "ban công" in data["detectedAmenities"]
    assert data["confidence"] >= 0.80

    # Kiểm tra object filters
    assert data["filters"]["max_price"] == 3000000.0
    assert data["filters"]["district"] == "Quận 1"
    assert "chỗ để xe" in data["filters"]["amenities"]


@pytest.mark.asyncio
async def test_nlp_parse_price_range_and_area(auth_headers):
    """Kiểm tra trích xuất khoảng giá và diện tích:
    'thuê phòng trọ từ 3 đến 5 triệu diện tích trên 25m2 ở Bình Thạnh có gác lửng'
    """
    payload = {"query": "thuê phòng trọ từ 3 đến 5 triệu diện tích trên 25m2 ở Bình Thạnh có gác lửng"}
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.post("/ai/nlp/parse-query", json=payload, headers=auth_headers)

    assert response.status_code == 200
    data = response.json()
    assert data["district"] == "Bình Thạnh"
    assert data["minPrice"] == 3000000.0
    assert data["maxPrice"] == 5000000.0
    assert data["minArea"] == 25.0
    assert "gác lửng" in data["detectedAmenities"]
    assert data["keyword"] == "phòng trọ"


@pytest.mark.asyncio
async def test_nlp_parse_colloquial_vietnamese(auth_headers):
    """Kiểm tra tiếng Việt khẩu ngữ: 'studio q7 tầm 4 củ có máy lạnh, nuôi mèo được'"""
    payload = {"query": "tìm studio q7 tầm 4 củ có máy lạnh, nuôi mèo được"}
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.post("/ai/nlp/parse-query", json=payload, headers=auth_headers)

    assert response.status_code == 200
    data = response.json()
    assert data["district"] == "Quận 7"
    assert data["maxPrice"] == 4000000.0
    assert "máy lạnh" in data["detectedAmenities"]
    assert "cho nuôi thú cưng" in data["detectedAmenities"]
    assert data["keyword"] == "studio"


@pytest.mark.asyncio
async def test_nlp_parse_ambiguous_query(auth_headers):
    """Kiểm tra câu tìm kiếm mơ hồ -> confidence thấp"""
    payload = {"query": "cần tìm phòng"}
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.post("/ai/nlp/parse-query", json=payload, headers=auth_headers)

    assert response.status_code == 200
    data = response.json()
    assert data["district"] is None
    assert data["maxPrice"] is None
    assert data["confidence"] <= 0.50
