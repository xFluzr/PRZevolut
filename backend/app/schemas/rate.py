from pydantic import BaseModel
from datetime import datetime
from typing import Optional


class RateOut(BaseModel):
    currency: str
    rate: float
    mid: float
    bid: Optional[float]
    ask: Optional[float]
    effective_date: Optional[str]
    fetched_at: datetime

    class Config:
        from_attributes = True
