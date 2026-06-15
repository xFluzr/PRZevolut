"""Endpointy autoryzacji — rejestracja, logowanie, refresh, wylogowanie."""

import datetime

from fastapi import APIRouter, Depends, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user
from app.core.exceptions import ConflictException, CredentialsException
from app.core.security import (
    create_access_token,
    create_refresh_token,
    hash_password,
    verify_password,
)
from app.database import get_db
from app.models import RefreshToken, User
from app.schemas import (
    AccessTokenResponse,
    LoginRequest,
    PasswordChangeRequest,
    RefreshRequest,
    RegisterRequest,
    TokenResponse,
    UserOut,
)

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post(
    "/register",
    response_model=TokenResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Rejestracja nowego użytkownika",
)
async def register(payload: RegisterRequest, db: AsyncSession = Depends(get_db)) -> TokenResponse:
    """Rejestruje nowego użytkownika i zwraca parę tokenów JWT."""
    # Sprawdź czy e-mail jest już zajęty
    existing = await db.execute(select(User).where(User.email == payload.email))
    if existing.scalar_one_or_none():
        raise ConflictException("Adres e-mail jest już zarejestrowany.")

    # Utwórz użytkownika z hashem bcrypt
    user = User(
        email=payload.email,
        password_hash=hash_password(payload.password),
    )
    db.add(user)
    await db.flush()  # Pobierz ID przed commitem

    # Wygeneruj tokeny
    access_token = create_access_token(user.id)
    refresh_token_str, expires_at = create_refresh_token()

    refresh_token = RefreshToken(
        user_id=user.id,
        token=refresh_token_str,
        expires_at=expires_at,
    )
    db.add(refresh_token)
    await db.commit()

    return TokenResponse(access_token=access_token, refresh_token=refresh_token_str)


@router.post(
    "/login",
    response_model=TokenResponse,
    summary="Logowanie użytkownika",
)
async def login(payload: LoginRequest, db: AsyncSession = Depends(get_db)) -> TokenResponse:
    """Weryfikuje dane logowania i zwraca parę tokenów JWT."""
    result = await db.execute(select(User).where(User.email == payload.email))
    user = result.scalar_one_or_none()

    if not user or not verify_password(payload.password, user.password_hash):
        raise CredentialsException("Nieprawidłowy e-mail lub hasło.")

    if not user.is_active:
        raise CredentialsException("Konto jest nieaktywne.")

    access_token = create_access_token(user.id)
    refresh_token_str, expires_at = create_refresh_token()

    refresh_token = RefreshToken(
        user_id=user.id,
        token=refresh_token_str,
        expires_at=expires_at,
    )
    db.add(refresh_token)
    await db.commit()

    return TokenResponse(access_token=access_token, refresh_token=refresh_token_str)


@router.post(
    "/refresh",
    response_model=AccessTokenResponse,
    summary="Odświeżenie access tokena",
)
async def refresh_token(payload: RefreshRequest, db: AsyncSession = Depends(get_db)) -> AccessTokenResponse:
    """Odświeżenie access tokena — unieważnia stary refresh token i generuje nowy access token"""
    result = await db.execute(
        select(RefreshToken).where(
            RefreshToken.token == payload.refresh_token,
            RefreshToken.revoked.is_(False),
        )
    )
    stored_token = result.scalar_one_or_none()

    if not stored_token:
        raise CredentialsException("Refresh token nieważny lub unieważniony.")

    now = datetime.datetime.now(datetime.timezone.utc)
    if stored_token.expires_at.replace(tzinfo=datetime.timezone.utc) < now:
        stored_token.revoked = True
        await db.commit()
        raise CredentialsException("Refresh token wygasł — zaloguj się ponownie.")

    # Unieważnij stary token (rotacja)
    stored_token.revoked = True

    # Wystawiaj nowy access token
    new_access_token = create_access_token(stored_token.user_id)
    await db.commit()

    return AccessTokenResponse(access_token=new_access_token)


@router.post(
    "/logout",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="Wylogowanie — unieważnienie refresh tokena",
)
async def logout(
    payload: RefreshRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> None:
    """Unieważnia refresh token użytkownika."""
    result = await db.execute(
        select(RefreshToken).where(
            RefreshToken.token == payload.refresh_token,
            RefreshToken.user_id == current_user.id,
        )
    )
    stored_token = result.scalar_one_or_none()
    if stored_token:
        stored_token.revoked = True
        await db.commit()


@router.get(
    "/me",
    response_model=UserOut,
    summary="Profil zalogowanego użytkownika",
)
async def get_me(current_user: User = Depends(get_current_user)) -> UserOut:
    """Zwraca dane konta bieżącego użytkownika."""
    return current_user


@router.patch(
    "/password",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="Zmiana hasła",
)
async def change_password(
    payload: PasswordChangeRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> None:
    """Zmienia hasło po podaniu aktualnego hasła."""
    if not verify_password(payload.current_password, current_user.password_hash):
        raise CredentialsException("Aktualne hasło jest nieprawidłowe.")

    current_user.password_hash = hash_password(payload.new_password)
    await db.commit()
