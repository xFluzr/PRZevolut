"""Logika bezpieczeństwa — bcrypt, JWT (access + refresh)."""

import datetime
import secrets

from jose import JWTError, jwt
from passlib.context import CryptContext

from app.config import get_settings

settings = get_settings()

pwd_context = CryptContext(
    schemes=["bcrypt"],
    deprecated="auto",
    bcrypt__rounds=settings.bcrypt_cost,
)


def hash_password(plain_password: str) -> str:
    """Hashuje hasło bcrypt z kosztem zdefiniowanym w ustawieniach."""
    return pwd_context.hash(plain_password)


def verify_password(plain_password: str, hashed_password: str) -> bool:
    """Sprawdza czy podane hasło pasuje do hasha."""
    return pwd_context.verify(plain_password, hashed_password)


def create_access_token(user_id: int) -> str:
    """Tworzy JWT access token ważny przez ACCESS_TOKEN_EXPIRE_MINUTES."""
    expire = datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(
        minutes=settings.access_token_expire_minutes
    )
    payload = {
        "sub": str(user_id),
        "type": "access",
        "exp": expire,
        "iat": datetime.datetime.now(datetime.timezone.utc),
    }
    return jwt.encode(payload, settings.secret_key, algorithm=settings.algorithm)


def create_refresh_token() -> tuple[str, datetime.datetime]:
    """Tworzy losowy refresh token i zwraca (token, expires_at)."""
    token = secrets.token_urlsafe(64)
    expires_at = datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(
        days=settings.refresh_token_expire_days
    )
    return token, expires_at


def decode_access_token(token: str) -> int | None:
    """Dekoduje JWT i zwraca user_id lub None jeśli token jest nieważny."""
    try:
        payload = jwt.decode(token, settings.secret_key, algorithms=[settings.algorithm])
        if payload.get("type") != "access":
            return None
        sub = payload.get("sub")
        return int(sub) if sub else None
    except (JWTError, ValueError):
        return None
