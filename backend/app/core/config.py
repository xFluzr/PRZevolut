from pydantic_settings import BaseSettings
from typing import List


class Settings(BaseSettings):
    APP_NAME: str = "PRZevolut API"
    DEBUG: bool = False
    API_VERSION: str = "v1"

    # Database
    DATABASE_URL: str = "sqlite:///./przevolut.db"

    # Security
    SECRET_KEY: str = "change-this-in-production"
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60
    REFRESH_TOKEN_EXPIRE_DAYS: int = 30

    # NBP API
    NBP_API_BASE_URL: str = "https://api.nbp.pl/api"
    TRACKED_CURRENCIES: str = "EUR,USD,GBP,CHF,CZK"
    RATE_FETCH_INTERVAL_MINUTES: int = 60

    # CORS
    ALLOWED_ORIGINS: str = "*"

    @property
    def tracked_currencies_list(self) -> List[str]:
        return [c.strip() for c in self.TRACKED_CURRENCIES.split(",")]

    @property
    def allowed_origins_list(self) -> List[str]:
        if self.ALLOWED_ORIGINS == "*":
            return ["*"]
        return [o.strip() for o in self.ALLOWED_ORIGINS.split(",")]

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


settings = Settings()
