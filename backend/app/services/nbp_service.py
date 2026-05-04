"""
NBP Open API service.

Pobiera kursy walut z publicznego API Narodowego Banku Polskiego.
Dokumentacja NBP API: https://api.nbp.pl/
"""
import httpx
import logging
from datetime import datetime, timezone
from sqlalchemy.orm import Session
from app.db.models import ExchangeRate
from app.core.config import settings

logger = logging.getLogger(__name__)

NBP_TABLE_A_URL = f"{settings.NBP_API_BASE_URL}/exchangerates/tables/A/?format=json"


async def fetch_rates_from_nbp() -> dict[str, dict]:
    """
    Pobiera tabelę A kursów walut z NBP API.
    Zwraca słownik: {kod_waluty: {mid, effectiveDate}}
    """
    async with httpx.AsyncClient(timeout=10.0) as client:
        response = await client.get(NBP_TABLE_A_URL)
        response.raise_for_status()

    data = response.json()
    rates_raw = data[0].get("rates", [])
    effective_date = data[0].get("effectiveDate", "")

    result = {}
    for item in rates_raw:
        code = item.get("code", "").upper()
        if code in settings.tracked_currencies_list:
            result[code] = {
                "mid": item["mid"],
                "effectiveDate": effective_date,
            }

    logger.info(f"[NBP] Pobrano kursy dla: {list(result.keys())} (data: {effective_date})")
    return result


def save_rates_to_db(db: Session, rates: dict[str, dict]) -> None:
    """Zapisuje pobrane kursy do bazy danych (Room cache)."""
    now = datetime.now(timezone.utc)
    for currency, data in rates.items():
        rate_entry = ExchangeRate(
            currency=currency,
            rate=data["mid"],
            mid=data["mid"],
            bid=None,
            ask=None,
            fetched_at=now,
            effective_date=data.get("effectiveDate"),
        )
        db.add(rate_entry)
    db.commit()
    logger.info(f"[DB] Zapisano {len(rates)} kursów do bazy.")


def get_latest_rates_from_db(db: Session) -> list[ExchangeRate]:
    """
    Zwraca najnowszy kurs dla każdej waluty z bazy danych.
    Używane jako fallback gdy NBP API jest niedostępne.
    """
    from sqlalchemy import func

    subquery = (
        db.query(
            ExchangeRate.currency,
            func.max(ExchangeRate.fetched_at).label("max_fetched")
        )
        .group_by(ExchangeRate.currency)
        .subquery()
    )

    return (
        db.query(ExchangeRate)
        .join(
            subquery,
            (ExchangeRate.currency == subquery.c.currency)
            & (ExchangeRate.fetched_at == subquery.c.max_fetched),
        )
        .all()
    )


async def refresh_rates(db: Session) -> None:
    """
    Zadanie cykliczne: pobierz kursy z NBP i zapisz do DB.
    Wywoływane przez APScheduler.
    """
    try:
        rates = await fetch_rates_from_nbp()
        save_rates_to_db(db, rates)
    except Exception as e:
        logger.error(f"[NBP] Błąd podczas pobierania kursów: {e}")
