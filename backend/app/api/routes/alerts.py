"""Endpointy CRUD alertów walutowych."""

from fastapi import APIRouter, Depends, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user, get_db
from app.core.exceptions import ForbiddenException, NotFoundException
from app.models import Alert, User
from app.schemas import AlertCreate, AlertOut, AlertUpdate

router = APIRouter(prefix="/alerts", tags=["alerts"])


@router.get(
    "",
    response_model=list[AlertOut],
    summary="Lista alertów użytkownika",
)
async def list_alerts(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> list[Alert]:
    """Zwraca listę wszystkich alertów zalogowanego użytkownika."""
    result = await db.execute(
        select(Alert)
        .where(Alert.user_id == current_user.id)
        .order_by(Alert.created_at.desc())
    )
    return list(result.scalars().all())


@router.post(
    "",
    response_model=AlertOut,
    status_code=status.HTTP_201_CREATED,
    summary="Utwórz nowy alert walutowy",
)
async def create_alert(
    payload: AlertCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> Alert:
    """Tworzy nowy alert walutowy dla zalogowanego użytkownika."""
    alert = Alert(
        user_id=current_user.id,
        currency_code=payload.currency_code,
        direction=payload.direction,
        threshold=payload.threshold,
    )
    db.add(alert)
    await db.commit()
    await db.refresh(alert)
    return alert


@router.patch(
    "/{alert_id}",
    response_model=AlertOut,
    summary="Edytuj alert walutowy",
)
async def update_alert(
    alert_id: int,
    payload: AlertUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> Alert:
    """Aktualizuje dane alertu walutowego. Można zmienić walutę, kierunek, próg lub status."""
    result = await db.execute(select(Alert).where(Alert.id == alert_id))
    alert = result.scalar_one_or_none()

    if alert is None:
        raise NotFoundException("Alert nie istnieje.")
    if alert.user_id != current_user.id:
        raise ForbiddenException("Nie masz uprawnień do edycji tego alertu.")

    # Aktualizuj tylko przekazane pola
    update_data = payload.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(alert, field, value)

    await db.commit()
    await db.refresh(alert)
    return alert


@router.delete(
    "/{alert_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="Usuń alert walutowy",
)
async def delete_alert(
    alert_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> None:
    """Usuwa alert walutowy użytkownika."""
    result = await db.execute(select(Alert).where(Alert.id == alert_id))
    alert = result.scalar_one_or_none()

    if alert is None:
        raise NotFoundException("Alert nie istnieje.")
    if alert.user_id != current_user.id:
        raise ForbiddenException("Nie masz uprawnień do usunięcia tego alertu.")

    await db.delete(alert)
    await db.commit()
