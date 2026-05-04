from pydantic import BaseModel
from datetime import datetime
from typing import Literal, Optional


class AlertCreate(BaseModel):
    currency: str                          # "EUR" | "USD" | "GBP" | "CHF" | "CZK"
    direction: Literal["below", "above"]  # poniżej / powyżej
    target_rate: float                     # docelowy kurs (PLN)


class AlertOut(BaseModel):
    id: int
    currency: str
    direction: str
    target_rate: float
    is_active: bool
    is_triggered: bool
    triggered_at: Optional[datetime]
    created_at: datetime

    class Config:
        from_attributes = True
