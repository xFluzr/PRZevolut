"""Konfiguracja aplikacji — pydantic-settings czyta zmienne środowiskowe."""

from functools import lru_cache
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")

    # Baza danych
    database_url: str = "postgresql+asyncpg://user:password@localhost:5432/przevolut"

    # JWT
    secret_key: str = "CHANGE_ME_USE_OPENSSL_RAND_HEX_32"
    algorithm: str = "HS256"
    access_token_expire_minutes: int = 15
    refresh_token_expire_days: int = 30

    # NBP
    nbp_api_url: str = "https://api.nbp.pl/api/exchangerates/tables/A?format=json"
    rate_refresh_interval_minutes: int = 60

    # CORS
    cors_origins: list[str] = ["http://localhost:3000"]

    # Firebase
    firebase_credentials_path: str = "firebase-credentials.json"

    # Bcrypt
    bcrypt_cost: int = 12

    # Aplikacja
    app_version: str = "1.0.0"


@lru_cache
def get_settings() -> Settings:
    """Zwraca singleton ustawień (cached po pierwszym wywołaniu)."""
    return Settings()
