from fastapi import HTTPException, Security, status
from fastapi.security.api_key import APIKeyHeader
from app.core.config import settings

API_KEY_HEADER = APIKeyHeader(name="X-Internal-Api-Key", auto_error=False)


async def verify_internal_api_key(api_key: str = Security(API_KEY_HEADER)) -> str:
    """
    Xác thực API Key nội bộ giữa Backend Spring Boot và AI Service FastAPI.
    Header yêu cầu: X-Internal-Api-Key
    """
    # Nếu không cấu hình key hoặc để trống (trong môi trường test linh hoạt), có thể bỏ qua
    expected_key = settings.INTERNAL_API_KEY

    if not api_key:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail={
                "success": False,
                "error_code": "MISSING_API_KEY",
                "message": "Thiếu header xác thực 'X-Internal-Api-Key'."
            }
        )

    if api_key != expected_key:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail={
                "success": False,
                "error_code": "INVALID_API_KEY",
                "message": "API key không hợp lệ hoặc không có quyền truy cập AI Service."
            }
        )

    return api_key
