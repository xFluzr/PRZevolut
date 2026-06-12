"""Dependencies FastAPI — get_current_user, get_db."""

from fastapi import Depends
from fastapi.security import OAuth2PasswordBearer
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import CredentialsException
from app.core.security import decode_access_token
from app.database import get_db
from app.models import User

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/auth/login")


async def get_current_user(
    token: str = Depends(oauth2_scheme),
    db: AsyncSession = Depends(get_db),
) -> User:
    """Dependency — zwraca zalogowanego użytkownika lub 401."""
    user_id = decode_access_token(token)
    if user_id is None:
        raise CredentialsException("Token nieważny lub wygasły.")

    result = await db.execute(select(User).where(User.id == user_id, User.is_active))
    user = result.scalar_one_or_none()
    if user is None:
        raise CredentialsException("Użytkownik nie istnieje.")
    return user
