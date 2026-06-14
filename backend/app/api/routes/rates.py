"""Endpointy kursów walut — aktualne i historia."""

import datetime

from fastapi import APIRouter, Depends, Query
from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user, get_db
from app.models import Rate, User
from app.schemas import RateHistoryItem, RateHistoryResponse, RateItem, RatesResponse
from app.services.nbp_client import fetch_nbp_rate_history

router = APIRouter(prefix="/rates", tags=["rates"])


@router.get(
    "",
    response_model=RatesResponse,
    summary="Pobierz aktualne kursy walut",
    description="Zwraca ostatni snapshot kursów z bazy (bufor z NBP). PLN jako waluta bazowa.",
)
async def get_rates(
    db: AsyncSession = Depends(get_db),
    _current_user: User = Depends(get_current_user),
) -> RatesResponse:
    """Zwraca aktualne kursy wszystkich walut (ostatni snapshot NBP)."""
    # Pobierz datę ostatniego snapshotu
    max_time_result = await db.execute(select(func.max(Rate.fetched_at)))
    latest_fetched_at: datetime.datetime | None = max_time_result.scalar()

    if latest_fetched_at is None:
        return RatesResponse(
            fetched_at=datetime.datetime.now(datetime.timezone.utc),
            rates=[],
        )

    # Pobierz wszystkie kursy z ostatniego snapshotu
    result = await db.execute(
        select(Rate).where(Rate.fetched_at == latest_fetched_at)
    )
    rates = result.scalars().all()

    return RatesResponse(
        fetched_at=latest_fetched_at,
        rates=[RateItem(code=r.code, name=r.name, rate=r.rate_to_pln) for r in rates],
    )


@router.get(
    "/history",
    response_model=RateHistoryResponse,
    summary="Historia kursu waluty",
    description="Zwraca serię czasową kursu dla podanego kodu waluty (np. EUR) z ostatnich N dni.",
)
async def get_rate_history(
    code: str = Query(min_length=3, max_length=3, description="Kod ISO waluty, np. EUR"),
    days: int = Query(default=14, ge=1, le=365, description="Liczba dni roboczych historii"),
    db: AsyncSession = Depends(get_db),
    _current_user: User = Depends(get_current_user),
) -> RateHistoryResponse:
    """Zwraca historię dziennych notowań z NBP (ostatnie N dni roboczych)."""
    currency = code.upper()
    nbp_history = await fetch_nbp_rate_history(currency, days)

    if nbp_history:
        return RateHistoryResponse(
            code=currency,
            days=days,
            history=[
                RateHistoryItem(rate=p.rate_to_pln, fetched_at=p.effective_date)
                for p in nbp_history
            ],
        )

    since = datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(days=days)
    result = await db.execute(
        select(Rate)
        .where(Rate.code == currency, Rate.fetched_at >= since)
        .order_by(Rate.fetched_at.asc())
    )
    rates = result.scalars().all()

    # Agreguj snapshoty godzinowe do jednego punktu na dzień
    daily: dict[datetime.date, Rate] = {}
    for rate in rates:
        daily[rate.fetched_at.date()] = rate

    return RateHistoryResponse(
        code=currency,
        days=days,
        history=[
            RateHistoryItem(
                rate=r.rate_to_pln,
                fetched_at=datetime.datetime.combine(
                    day, datetime.time(12, 0), tzinfo=datetime.timezone.utc
                ),
            )
            for day, r in sorted(daily.items())
        ][-days:],
    )
