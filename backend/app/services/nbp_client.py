"""Klient NBP — pobiera kursy walutowe z api.nbp.pl."""

import datetime
import logging
from dataclasses import dataclass

import httpx

from app.config import get_settings

logger = logging.getLogger(__name__)
settings = get_settings()


@dataclass
class NbpRate:
    """Pojedynczy kurs waluty z NBP."""
    code: str
    name: str
    rate_to_pln: float
    fetched_at: datetime.datetime


async def fetch_nbp_rates() -> list[NbpRate]:
    """
    Pobiera tabelę A kursów walut z api.nbp.pl.
    Zwraca listę NbpRate lub pustą listę w razie błędu.
    """
    fetched_at = datetime.datetime.now(datetime.timezone.utc)

    async with httpx.AsyncClient(timeout=30.0) as client:
        try:
            response = await client.get(
                settings.nbp_api_url,
                headers={"Accept": "application/json"},
            )
            response.raise_for_status()
            data = response.json()

            rates: list[NbpRate] = []
            # NBP zwraca listę tabel; bierzemy pierwszą tabelę A
            for table in data:
                for rate_data in table.get("rates", []):
                    rates.append(
                        NbpRate(
                            code=rate_data["code"],
                            name=rate_data["currency"],
                            rate_to_pln=rate_data["mid"],
                            fetched_at=fetched_at,
                        )
                    )

            logger.info("Pobrano %d kursów z NBP (snapshot: %s)", len(rates), fetched_at)
            return rates

        except httpx.HTTPError as exc:
            logger.error("Błąd HTTP podczas pobierania kursów NBP: %s", exc)
            return []
        except Exception as exc:
            logger.exception("Nieoczekiwany błąd podczas pobierania kursów NBP: %s", exc)
            return []
