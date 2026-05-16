"""Agregator kursów — APScheduler co 60 minut pobiera i zapisuje snapshot NBP."""

import logging

from apscheduler.schedulers.asyncio import AsyncIOScheduler
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import AsyncSessionLocal
from app.models import Rate
from app.services.nbp_client import fetch_nbp_rates

logger = logging.getLogger(__name__)

# Globalny scheduler — uruchamiany przy starcie aplikacji
_scheduler = AsyncIOScheduler()


async def aggregate_rates() -> None:
    """
    Pobiera snapshot kursów z NBP i zapisuje je do bazy.
    Wywoływana przez APScheduler co RATE_REFRESH_INTERVAL_MINUTES minut.
    """
    logger.info("Rozpoczęcie agregacji kursów NBP...")
    rates = await fetch_nbp_rates()

    if not rates:
        logger.warning("Brak danych z NBP — snapshot pominięty.")
        return

    async with AsyncSessionLocal() as db:
        try:
            db_rates = [
                Rate(
                    code=r.code,
                    name=r.name,
                    rate_to_pln=r.rate_to_pln,
                    fetched_at=r.fetched_at,
                )
                for r in rates
            ]
            db.add_all(db_rates)
            await db.commit()
            logger.info("Zapisano %d kursów do bazy (snapshot).", len(db_rates))

            # Po zapisaniu kursów uruchom silnik alertów
            from app.services.alert_engine import process_alerts
            await process_alerts(rates)

        except Exception as exc:
            logger.exception("Błąd podczas zapisywania kursów: %s", exc)
            await db.rollback()


def start_scheduler(interval_minutes: int = 60) -> None:
    """Uruchamia scheduler APScheduler z podanym interwałem (minuty)."""
    _scheduler.add_job(
        aggregate_rates,
        trigger="interval",
        minutes=interval_minutes,
        id="nbp_aggregator",
        replace_existing=True,
        max_instances=1,
    )
    _scheduler.start()
    logger.info("Scheduler NBP uruchomiony — interwał: %d min.", interval_minutes)


def stop_scheduler() -> None:
    """Zatrzymuje scheduler przy zamknięciu aplikacji."""
    if _scheduler.running:
        _scheduler.shutdown(wait=False)
        logger.info("Scheduler NBP zatrzymany.")
