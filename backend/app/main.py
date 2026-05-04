"""
PRZevolut API — punkt wejścia aplikacji FastAPI.

Uruchomienie:
    uvicorn app.main:app --reload

Swagger UI: http://localhost:8000/docs
ReDoc:      http://localhost:8000/redoc
"""
import asyncio
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from apscheduler.schedulers.asyncio import AsyncIOScheduler
from apscheduler.triggers.cron import CronTrigger

from app.core.config import settings
from app.db.database import Base, engine, SessionLocal
from app.api import auth, rates, alerts
from app.services.nbp_service import refresh_rates

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# ── Scheduler (cykliczne pobieranie kursów NBP) ────────────────────────────
scheduler = AsyncIOScheduler()


async def scheduled_rate_fetch():
    """Zadanie cykliczne: odświeża kursy walut z NBP API."""
    db = SessionLocal()
    try:
        await refresh_rates(db)
    finally:
        db.close()


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Lifecycle hook: uruchamiany przy starcie i zatrzymaniu aplikacji."""
    # Tworzymy tabele jeśli nie istnieją (dev mode; produkcja używa Alembic)
    Base.metadata.create_all(bind=engine)
    logger.info("[DB] Tabele zainicjalizowane.")

    # Pobierz kursy przy starcie
    await scheduled_rate_fetch()

    # Scheduler: codziennie w dni robocze co godzinę 8-18
    scheduler.add_job(
        scheduled_rate_fetch,
        CronTrigger(day_of_week="mon-fri", hour="8-18", minute=0),
        id="fetch_rates",
        replace_existing=True,
    )
    scheduler.start()
    logger.info("[Scheduler] Uruchomiony — kursy NBP będą odświeżane co godzinę (Pn-Pt 8-18).")

    yield  # Aplikacja działa

    scheduler.shutdown()
    logger.info("[Scheduler] Zatrzymany.")


# ── Aplikacja FastAPI ──────────────────────────────────────────────────────
app = FastAPI(
    title=settings.APP_NAME,
    description=(
        "REST API dla aplikacji PRZevolut — Walutowy Skaner AR.\n\n"
        "Obsługuje: rejestrację/logowanie (JWT), kursy walut NBP, alerty walutowe."
    ),
    version="1.0.0",
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc",
)

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.allowed_origins_list,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ── Routery ───────────────────────────────────────────────────────────────
app.include_router(auth.router)
app.include_router(rates.router)
app.include_router(alerts.router)


@app.get("/", tags=["health"])
def health_check():
    """Health check endpoint."""
    return {"status": "ok", "app": settings.APP_NAME, "version": "1.0.0"}
