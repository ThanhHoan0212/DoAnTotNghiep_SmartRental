from pathlib import Path
from typing import List
from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore"
    )

    # General App Config
    PROJECT_NAME: str = "SmartRental AI Service"
    VERSION: str = "1.0.0"
    ENVIRONMENT: str = "development"
    DEBUG: bool = True

    # Server Network Config
    HOST: str = "0.0.0.0"
    PORT: int = 8000
    API_V1_STR: str = "/ai"

    # Security: Internal API Key for Spring Boot Backend communication
    INTERNAL_API_KEY: str = "smartrental-ai-secret-key-2026"

    # CORS Origins (Allow Spring Boot backend & Frontend if needed)
    CORS_ORIGINS: List[str] = [
        "http://localhost:8080",
        "http://localhost:5173",
        "http://localhost:3000",
        "http://127.0.0.1:8080",
        "http://127.0.0.1:5173",
        "*"
    ]

    # Directories
    BASE_DIR: Path = Path(__file__).resolve().parent.parent.parent
    DATA_DIR: Path = BASE_DIR / "data"
    MODELS_DIR: Path = BASE_DIR / "app" / "models"


settings = Settings()
