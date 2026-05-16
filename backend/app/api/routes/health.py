"""Endpointy rejestracji urządzeń FCM i healthcheck."""

from fastapi import APIRouter, Depends, status
from sqlalchemy import select, text
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user, get_db
from app.config import get_settings
from app.models import DeviceToken, User
from app.schemas import DeviceRegisterRequest, DeviceRegisterResponse, HealthResponse

settings = get_settings()

router_devices = APIRouter(prefix="/devices", tags=["devices"])
router_health = APIRouter(tags=["health"])


@router_devices.post(
    "/register",
    response_model=DeviceRegisterResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Rejestracja tokena FCM urządzenia",
)
async def register_device(
    payload: DeviceRegisterRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> DeviceToken:
    """Rejestruje token FCM urządzenia użytkownika do powiadomień push."""
    # Sprawdź czy token już istnieje (aktualizuj user_id jeśli tak)
    result = await db.execute(
        select(DeviceToken).where(DeviceToken.fcm_token == payload.fcm_token)
    )
    existing = result.scalar_one_or_none()

    if existing:
        existing.user_id = current_user.id
        existing.platform = payload.platform
        await db.commit()
        await db.refresh(existing)
        return existing

    device_token = DeviceToken(
        user_id=current_user.id,
        fcm_token=payload.fcm_token,
        platform=payload.platform,
    )
    db.add(device_token)
    await db.commit()
    await db.refresh(device_token)
    return device_token


@router_health.get(
    "/health",
    response_model=HealthResponse,
    summary="Healthcheck",
    description="Sprawdza stan aplikacji i połączenie z bazą danych. Używany przez Render.com.",
)
async def health_check(db: AsyncSession = Depends(get_db)) -> HealthResponse:
    """Healthcheck pod Render.com — sprawdza połączenie z DB."""
    try:
        await db.execute(text("SELECT 1"))
        db_status = "connected"
    except Exception:
        db_status = "error"

    return HealthResponse(
        status="ok",
        version=settings.app_version,
        db=db_status,
    )
