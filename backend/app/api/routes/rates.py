"""Endpointy kursów walut — aktualne i historia."""

import datetime

from fastapi import APIRouter, Depends, Query
from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user, get_db
from app.models import Rate, User
from app.schemas import RateHistoryItem, RateHistoryResponse, RateItem, RatesResponse

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
    days: int = Query(default=30, ge=1, le=365, description="Liczba dni historii"),
    db: AsyncSession = Depends(get_db),
    _current_user: User = Depends(get_current_user),
) -> RateHistoryResponse:
    """Zwraca historię kursu podanej waluty z ostatnich N dni."""
    since = datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(days=days)

    result = await db.execute(
        select(Rate)
        .where(Rate.code == code.upper(), Rate.fetched_at >= since)
        .order_by(Rate.fetched_at.desc())
    )
    rates = result.scalars().all()

    return RateHistoryResponse(
        code=code.upper(),
        days=days,
        history=[RateHistoryItem(rate=r.rate_to_pln, fetched_at=r.fetched_at) for r in rates],
    )
