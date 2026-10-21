import pytest
from httpx import ASGITransport, AsyncClient
from app.main import app
from app.core.config import settings


@pytest.fixture
def auth_headers():
    return {"X-Internal-Api-Key": settings.INTERNAL_API_KEY}


@pytest.mark.asyncio
async def test_fraud_check_unauthorized():
    """Kiểm tra chặn truy cập nếu không có API key nội bộ"""
    payload = {
        "room_id": 1,
        "title": "Phòng trọ giá rẻ",
        "description": "Mô tả phòng trọ bình thường",
        "price": 3000000,
        "district": "Quận 10",
        "area": 20
    }
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.post("/ai/fraud/check-listing", json=payload)
    assert response.status_code == 401
    assert response.json()["error_code"] == "MISSING_API_KEY"


@pytest.mark.asyncio
async def test_fraud_check_normal_listing(auth_headers):
    """Kiểm tra tin đăng bình thường, giá hợp lý thị trường -> LOW risk"""
    payload = {
        "room_id": 100,
        "title": "Cho thuê phòng trọ khép kín mới xây tại Quận 7",
        "description": "Phòng có diện tích 25m2, trang bị sẵn máy lạnh, tủ quần áo, nệm cao cấp. Có bảo vệ trông xe, khóa vân tay, camera an ninh 24/7.",
        "price": 4000000.0,
        "district": "Quận 7",
        "area": 25.0
    }
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.post("/ai/fraud/check-listing", json=payload, headers=auth_headers)

    assert response.status_code == 200
    data = response.json()
    assert data["room_id"] == 100
    assert data["risk_level"] == "LOW"
    assert data["is_fraud"] is False
    assert data["fraud_score"] < 0.35


@pytest.mark.asyncio
async def test_fraud_check_extreme_low_price(auth_headers):
    """Kiểm tra tin đăng giá rẻ bất thường (mồi nhử lừa cọc) -> HIGH risk"""
    payload = {
        "room_id": 101,
        "title": "Căn hộ cao cấp luxury view đẹp trung tâm Quận 1",
        "description": "Phòng siêu đẹp full nội thất cao cấp tại Quận 1, diện tích 35m2 đầy đủ tiện nghi vip cần cho thuê gấp.",
        "price": 1200000.0,  # Giá cực rẻ so với Quận 1 (bình thường 8-12 triệu)
        "district": "Quận 1",
        "area": 35.0
    }
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.post("/ai/fraud/check-listing", json=payload, headers=auth_headers)

    assert response.status_code == 200
    data = response.json()
    assert data["risk_level"] == "HIGH"
    assert data["is_fraud"] is True
    assert data["fraud_score"] >= 0.65
    assert any("thấp bất thường" in r for r in data["reasons"])


@pytest.mark.asyncio
async def test_fraud_check_suspicious_deposit_phrases(auth_headers):
    """Kiểm tra tin đăng có từ khóa hối thúc cọc Zalo -> HIGH / MEDIUM risk"""
    payload = {
        "room_id": 102,
        "title": "Phòng trọ đẹp giá tốt cọc ngay",
        "description": "Phòng trọ 20m2 ở Bình Thạnh giá tốt. Yêu cầu chuyển khoản cọc trước qua Zalo để giữ phòng gấp kẻo mất nhé.",
        "price": 3500000.0,
        "district": "Bình Thạnh",
        "area": 20.0
    }
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.post("/ai/fraud/check-listing", json=payload, headers=auth_headers)

    assert response.status_code == 200
    data = response.json()
    assert data["is_fraud"] is True
    assert any("cọc trước qua Zalo" in r for r in data["reasons"])


@pytest.mark.asyncio
async def test_fraud_check_duplicate_content(auth_headers):
    """Kiểm tra phát hiện trùng lặp nội dung mô tả qua TF-IDF Cosine Similarity"""
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        # 1. Đồng bộ 1 tin mẫu vào corpus
        unique_text = "Căn phòng studio phong cách Scandinavia độc bản tại Thảo Điền view Landmark 81 cực chill."
        sync_payload = {
            "room_id": 8888,
            "title": "Studio Scandinavia độc bản Thảo Điền",
            "description": unique_text,
            "price": 7500000.0,
            "district": "Thủ Đức",
            "area": 32.0
        }
        sync_res = await ac.post("/ai/fraud/sync-listing", json=sync_payload, headers=auth_headers)
        assert sync_res.status_code == 200

        # 2. Gửi tin mới sao chép gần như 100% nội dung trên
        copycat_payload = {
            "room_id": 9999,
            "title": "Studio Scandinavia độc bản Thảo Điền",
            "description": unique_text + " Liên hệ ngay.",
            "price": 7500000.0,
            "district": "Thủ Đức",
            "area": 32.0
        }
        check_res = await ac.post("/ai/fraud/check-listing", json=copycat_payload, headers=auth_headers)
        assert check_res.status_code == 200
        check_data = check_res.json()
        assert check_data["is_fraud"] is True
        assert any("trùng lặp" in r for r in check_data["reasons"])
