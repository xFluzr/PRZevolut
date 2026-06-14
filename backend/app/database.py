"""Konfiguracja bazy danych SQLAlchemy async."""

from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from sqlalchemy.orm import DeclarativeBase

from app.config import get_settings

settings = get_settings()

_engine_kwargs: dict = {"echo": False, "pool_pre_ping": True}
if settings.database_url.startswith("postgresql"):
    _engine_kwargs.update(pool_size=10, max_overflow=20)

engine = create_async_engine(
    settings.database_url,
    **_engine_kwargs,
)

AsyncSessionLocal = async_sessionmaker(
    bind=engine,
    class_=AsyncSession,
    expire_on_commit=False,
    autoflush=False,
    autocommit=False,
)


class Base(DeclarativeBase):
    """Bazowa klasa dla wszystkich modeli SQLAlchemy."""
    pass


async def get_db() -> AsyncSession:
    """Dependency FastAPI — dostarcza sesję bazy danych."""
    async with AsyncSessionLocal() as session:
        try:
            yield session
            await session.commit()
        except Exception:
            await session.rollback()
            raise
        finally:
            await session.close()
