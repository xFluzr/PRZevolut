"""Główna aplikacja FastAPI — PRZevolut backend."""

import logging
from contextlib import asynccontextmanager
from typing import AsyncGenerator

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.errors import RateLimitExceeded
from slowapi.util import get_remote_address

from app.api.routes.alerts import router as alerts_router
from app.api.routes.auth import router as auth_router
from app.api.routes.health import router_devices, router_health
from app.api.routes.rates import router as rates_router
from app.config import get_settings
from app.services.rate_aggregator import start_scheduler, stop_scheduler

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

settings = get_settings()

# Rate limiter (slowapi)
limiter = Limiter(key_func=get_remote_address)


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncGenerator[None, None]:
    """Lifecycle hook — uruchom scheduler przy starcie, zatrzymaj przy zamknięciu."""
    logger.info("PRZevolut backend startuje...")
    start_scheduler(interval_minutes=settings.rate_refresh_interval_minutes)

    yield  # Aplikacja działa

    logger.info("PRZevolut backend zatrzymuje się...")
    stop_scheduler()


app = FastAPI(
    title="PRZevolut API",
    description="Backend walutowego skanera AR PRZevolut — kursy NBP, alerty, autoryzacja JWT.",
    version=settings.app_version,
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc",
)

# Rate limiting
app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Rejestracja routerów
app.include_router(auth_router)
app.include_router(rates_router)
app.include_router(alerts_router)
app.include_router(router_devices)
app.include_router(router_health)


@app.get("/", include_in_schema=False)
async def root() -> dict:
    """Root redirect do dokumentacji."""
    return {"message": "PRZevolut API v1.0.0 — dokumentacja: /docs"}
