"""
Endpointy autoryzacji: rejestracja i logowanie.

POST /auth/register  — tworzy nowe konto
POST /auth/login     — loguje i zwraca tokeny JWT
"""
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.db.database import get_db
from app.db.models import User
from app.schemas.user import UserCreate, UserLogin, UserOut, TokenOut
from app.core.security import hash_password, verify_password, create_access_token, create_refresh_token

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post("/register", response_model=UserOut, status_code=status.HTTP_201_CREATED)
def register(user_data: UserCreate, db: Session = Depends(get_db)):
    """Rejestracja nowego użytkownika."""
    existing = db.query(User).filter(User.email == user_data.email).first()
    if existing:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Konto z tym adresem e-mail już istnieje."
        )

    user = User(
        email=user_data.email,
        hashed_password=hash_password(user_data.password)
    )
    db.add(user)
    db.commit()
    db.refresh(user)
    return user


@router.post("/login", response_model=TokenOut)
def login(credentials: UserLogin, db: Session = Depends(get_db)):
    """Logowanie — weryfikuje hasło i zwraca access + refresh token."""
    user = db.query(User).filter(User.email == credentials.email).first()
    if not user or not verify_password(credentials.password, user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Nieprawidłowy e-mail lub hasło.",
            headers={"WWW-Authenticate": "Bearer"},
        )

    if not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Konto jest nieaktywne."
        )

    token_data = {"sub": str(user.id), "email": user.email}
    return TokenOut(
        access_token=create_access_token(token_data),
        refresh_token=create_refresh_token(token_data),
    )
