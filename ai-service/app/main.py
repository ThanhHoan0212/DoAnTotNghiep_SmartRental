import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException, Request, Depends, status
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, RedirectResponse

from app.core.config import settings
from app.core.security import verify_internal_api_key
from app.schemas.common import HealthCheckResponse, StandardErrorResponse
from app.api.fraud_router import router as fraud_router
from app.api.recommendation_router import router as recommendation_router
from app.api.nlp_router import router as nlp_router
from app.api.chatbot_router import router as chatbot_router
from app.api.price_prediction_router import router as price_prediction_router

# Cấu hình logging định dạng JSON / có cấu trúc
logging.basicConfig(
    level=logging.INFO if not settings.DEBUG else logging.DEBUG,
    format='{"time": "%(asctime)s", "level": "%(levelname)s", "module": "%(name)s", "message": "%(message)s"}'
)
logger = logging.getLogger("ai_service")


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Khởi động AI Service
    logger.info("Initializing SmartRental AI Service...")
    logger.info("Service Environment: %s | Port: %s", settings.ENVIRONMENT, settings.PORT)
    yield
    # Dọn dẹp khi shutdown
    logger.info("Shutting down SmartRental AI Service...")


app = FastAPI(
    title=settings.PROJECT_NAME,
    version=settings.VERSION,
    description="Hệ thống AI Service độc lập hỗ trợ: NLP Parse câu hỏi tự nhiên, Chatbot, Gợi ý phòng trọ, Phát hiện gian lận và Dự đoán xu hướng giá.",
    docs_url="/docs",
    redoc_url="/redoc",
    openapi_url="/openapi.json",
    lifespan=lifespan
)

# Cấu hình CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ==========================================
# Global Exception Handlers (Thống nhất JSON)
# ==========================================

@app.exception_handler(HTTPException)
async def http_exception_handler(request: Request, exc: HTTPException):
    """Xử lý HTTPException chuẩn hóa theo format đồ án"""
    if isinstance(exc.detail, dict):
        content = exc.detail
        if "success" not in content:
            content["success"] = False
        return JSONResponse(status_code=exc.status_code, content=content)

    return JSONResponse(
        status_code=exc.status_code,
        content={
            "success": False,
            "error_code": f"HTTP_{exc.status_code}",
            "message": str(exc.detail)
        }
    )


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    """Bắt lỗi validation của Pydantic v2"""
    return JSONResponse(
        status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
        content={
            "success": False,
            "error_code": "VALIDATION_ERROR",
            "message": "Dữ liệu đầu vào không hợp lệ hoặc sai định dạng.",
            "detail": exc.errors()
        }
    )


@app.exception_handler(Exception)
async def general_exception_handler(request: Request, exc: Exception):
    """Bắt các lỗi ngoại lệ không mong muốn khác"""
    logger.error("Unhandled Exception: %s", str(exc), exc_info=True)
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={
            "success": False,
            "error_code": "INTERNAL_SERVER_ERROR",
            "message": f"Lỗi nội bộ AI Service: {str(exc)}"
        }
    )


# ==========================================
# Root & Health-check Endpoints
# ==========================================

@app.get("/", include_in_schema=False)
async def root_redirect():
    """Chuyển hướng trang chủ về tài liệu Swagger UI /docs"""
    return RedirectResponse(url="/docs")


@app.get(
    "/health",
    response_model=HealthCheckResponse,
    tags=["Health"],
    summary="Kiểm tra trạng thái hoạt động của AI Service"
)
async def health_check():
    """Endpoint kiểm tra sức khỏe liveness/readiness cho Docker / Backend"""
    return HealthCheckResponse(
        status="ok",
        service=settings.PROJECT_NAME,
        version=settings.VERSION,
        environment=settings.ENVIRONMENT
    )


@app.get(
    f"{settings.API_V1_STR}/health/secure",
    response_model=HealthCheckResponse,
    tags=["Health"],
    summary="Kiểm tra kết nối nội bộ có xác thực API Key"
)
async def secure_health_check(_api_key: str = Depends(verify_internal_api_key)):
    """Endpoint test xác thực header X-Internal-Api-Key giữa Backend và AI Service"""
    return HealthCheckResponse(
        status="authenticated",
        service=settings.PROJECT_NAME,
        version=settings.VERSION,
        environment=settings.ENVIRONMENT
    )


# ==========================================
# Include Feature Routers
# ==========================================
app.include_router(fraud_router, prefix=settings.API_V1_STR)
app.include_router(recommendation_router, prefix=settings.API_V1_STR)
app.include_router(nlp_router, prefix=settings.API_V1_STR)
app.include_router(chatbot_router, prefix=settings.API_V1_STR)
app.include_router(price_prediction_router, prefix=settings.API_V1_STR)




