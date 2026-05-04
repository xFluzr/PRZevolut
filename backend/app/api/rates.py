"""
Endpointy kursów walut.

GET /rates           — zwraca aktualne kursy (z DB lub świeże z NBP)
GET /rates/{currency} — kurs konkretnej waluty
"""
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from typing import List
from app.db.database import get_db
from app.schemas.rate import RateOut
from app.services.nbp_service import get_latest_rates_from_db
from app.core.config import settings

router = APIRouter(prefix="/rates", tags=["rates"])


@router.get("", response_model=List[RateOut])
def get_all_rates(db: Session = Depends(get_db)):
    """
    Zwraca najnowszy kurs dla każdej śledzonej waluty.
    Dane pochodzą z lokalnej bazy (cache odświeżany przez scheduler).
    """
    rates = get_latest_rates_from_db(db)
    if not rates:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Kursy walut nie są jeszcze dostępne. Spróbuj ponownie za chwilę."
        )
    return rates


@router.get("/{currency}", response_model=RateOut)
def get_rate_by_currency(currency: str, db: Session = Depends(get_db)):
    """Zwraca aktualny kurs dla podanej waluty (np. EUR, USD)."""
    currency = currency.upper()
    if currency not in settings.tracked_currencies_list:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Waluta '{currency}' nie jest obsługiwana. Dostępne: {settings.TRACKED_CURRENCIES}"
        )

    rates = get_latest_rates_from_db(db)
    for rate in rates:
        if rate.currency == currency:
            return rate

    raise HTTPException(
        status_code=status.HTTP_404_NOT_FOUND,
        detail=f"Kurs dla waluty '{currency}' nie jest jeszcze dostępny."
    )
