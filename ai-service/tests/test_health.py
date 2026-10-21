import pytest
from httpx import ASGITransport, AsyncClient
from app.main import app
from app.core.config import settings


@pytest.mark.asyncio
async def test_health_check_public():
    """Test public /health endpoint without any headers"""
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "ok"
    assert data["service"] == settings.PROJECT_NAME
    assert "timestamp" in data


@pytest.mark.asyncio
async def test_secure_health_check_missing_key():
    """Test /ai/health/secure without X-Internal-Api-Key returns 401"""
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.get(f"{settings.API_V1_STR}/health/secure")
    assert response.status_code == 401
    data = response.json()
    assert data["success"] is False
    assert data["error_code"] == "MISSING_API_KEY"


@pytest.mark.asyncio
async def test_secure_health_check_invalid_key():
    """Test /ai/health/secure with wrong X-Internal-Api-Key returns 403"""
    headers = {"X-Internal-Api-Key": "wrong-secret-key"}
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.get(f"{settings.API_V1_STR}/health/secure", headers=headers)
    assert response.status_code == 403
    data = response.json()
    assert data["success"] is False
    assert data["error_code"] == "INVALID_API_KEY"


@pytest.mark.asyncio
async def test_secure_health_check_valid_key():
    """Test /ai/health/secure with valid X-Internal-Api-Key returns 200"""
    headers = {"X-Internal-Api-Key": settings.INTERNAL_API_KEY}
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.get(f"{settings.API_V1_STR}/health/secure", headers=headers)
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "authenticated"
