"""Schematy Pydantic v2 — walidacja request/response."""

import datetime
from pydantic import BaseModel, EmailStr, Field, field_validator


# ─── Auth ────────────────────────────────────────────────────────────────────

class RegisterRequest(BaseModel):
    email: EmailStr
    password: str = Field(min_length=8, max_length=128)


class LoginRequest(BaseModel):
    email: EmailStr
    password: str


class TokenResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"


class RefreshRequest(BaseModel):
    refresh_token: str


class AccessTokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"


# ─── User ────────────────────────────────────────────────────────────────────

class UserOut(BaseModel):
    id: int
    email: str
    created_at: datetime.datetime
    is_active: bool

    model_config = {"from_attributes": True}


class PasswordChangeRequest(BaseModel):
    current_password: str
    new_password: str = Field(min_length=8, max_length=128)


# ─── Rates ───────────────────────────────────────────────────────────────────

class RateItem(BaseModel):
    code: str
    name: str
    rate: float

    model_config = {"from_attributes": True}


class RatesResponse(BaseModel):
    fetched_at: datetime.datetime
    base: str = "PLN"
    rates: list[RateItem]


class RateHistoryItem(BaseModel):
    rate: float
    fetched_at: datetime.datetime

    model_config = {"from_attributes": True}


class RateHistoryResponse(BaseModel):
    code: str
    days: int
    history: list[RateHistoryItem]


# ─── Alerts ──────────────────────────────────────────────────────────────────

class AlertCreate(BaseModel):
    currency_code: str = Field(min_length=3, max_length=3)
    direction: str = Field(pattern="^(above|below)$")
    threshold: float = Field(gt=0)

    @field_validator("currency_code")
    @classmethod
    def currency_code_upper(cls, v: str) -> str:
        return v.upper()


class AlertUpdate(BaseModel):
    currency_code: str | None = Field(default=None, min_length=3, max_length=3)
    direction: str | None = Field(default=None, pattern="^(above|below)$")
    threshold: float | None = Field(default=None, gt=0)
    is_active: bool | None = None

    @field_validator("currency_code")
    @classmethod
    def currency_code_upper(cls, v: str | None) -> str | None:
        return v.upper() if v else None


class AlertOut(BaseModel):
    id: int
    currency_code: str
    direction: str
    threshold: float
    is_active: bool
    last_triggered_at: datetime.datetime | None
    created_at: datetime.datetime

    model_config = {"from_attributes": True}


# ─── Devices ─────────────────────────────────────────────────────────────────

class DeviceRegisterRequest(BaseModel):
    fcm_token: str = Field(min_length=10)
    platform: str = Field(default="android", pattern="^(android|ios)$")


class DeviceRegisterResponse(BaseModel):
    id: int
    platform: str
    created_at: datetime.datetime

    model_config = {"from_attributes": True}


# ─── Health ──────────────────────────────────────────────────────────────────

class HealthResponse(BaseModel):
    status: str
    version: str
    db: str
