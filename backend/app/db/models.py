from sqlalchemy import Column, Integer, String, Float, Boolean, DateTime, ForeignKey
from sqlalchemy.orm import relationship
from datetime import datetime, timezone
from app.db.database import Base


class User(Base):
    """Konto użytkownika."""
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    email = Column(String, unique=True, index=True, nullable=False)
    hashed_password = Column(String, nullable=False)
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime, default=lambda: datetime.now(timezone.utc))

    alerts = relationship("Alert", back_populates="owner", cascade="all, delete-orphan")


class Alert(Base):
    """Alert walutowy użytkownika (np. EUR < 4.20 PLN)."""
    __tablename__ = "alerts"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    currency = Column(String(3), nullable=False)       # np. "EUR"
    direction = Column(String(5), nullable=False)      # "below" | "above"
    target_rate = Column(Float, nullable=False)
    is_active = Column(Boolean, default=True)
    is_triggered = Column(Boolean, default=False)
    triggered_at = Column(DateTime, nullable=True)
    created_at = Column(DateTime, default=lambda: datetime.now(timezone.utc))

    owner = relationship("User", back_populates="alerts")


class ExchangeRate(Base):
    """Kurs waluty pobrany z NBP API (cache)."""
    __tablename__ = "exchange_rates"

    id = Column(Integer, primary_key=True, index=True)
    currency = Column(String(3), index=True, nullable=False)   # np. "EUR"
    rate = Column(Float, nullable=False)                       # kurs do PLN
    mid = Column(Float, nullable=False)                        # kurs średni NBP
    bid = Column(Float, nullable=True)                         # kurs kupna
    ask = Column(Float, nullable=True)                         # kurs sprzedaży
    fetched_at = Column(DateTime, default=lambda: datetime.now(timezone.utc), index=True)
    effective_date = Column(String(10), nullable=True)         # np. "2026-05-04"
