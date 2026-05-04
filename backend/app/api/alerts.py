"""
Endpointy alertów walutowych.

GET    /alerts        — lista alertów zalogowanego użytkownika
POST   /alerts        — tworzenie nowego alertu
DELETE /alerts/{id}   — usunięcie alertu
"""
from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from sqlalchemy.orm import Session
from typing import List
from jose import JWTError
from app.db.database import get_db
from app.db.models import Alert, User
from app.schemas.alert import AlertCreate, AlertOut
from app.core.security import decode_token

router = APIRouter(prefix="/alerts", tags=["alerts"])
security = HTTPBearer()


def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(security),
    db: Session = Depends(get_db)
) -> User:
    """Dependency: waliduje JWT i zwraca zalogowanego użytkownika."""
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Nieprawidłowy lub wygasły token.",
        headers={"WWW-Authenticate": "Bearer"},
    )
    try:
        payload = decode_token(credentials.credentials)
        user_id: str = payload.get("sub")
        if user_id is None:
            raise credentials_exception
    except JWTError:
        raise credentials_exception

    user = db.query(User).filter(User.id == int(user_id)).first()
    if user is None or not user.is_active:
        raise credentials_exception
    return user


@router.get("", response_model=List[AlertOut])
def list_alerts(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Zwraca wszystkie alerty zalogowanego użytkownika."""
    return db.query(Alert).filter(Alert.user_id == current_user.id).all()


@router.post("", response_model=AlertOut, status_code=status.HTTP_201_CREATED)
def create_alert(
    alert_data: AlertCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Tworzy nowy alert walutowy."""
    alert = Alert(
        user_id=current_user.id,
        currency=alert_data.currency.upper(),
        direction=alert_data.direction,
        target_rate=alert_data.target_rate,
    )
    db.add(alert)
    db.commit()
    db.refresh(alert)
    return alert


@router.delete("/{alert_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_alert(
    alert_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Usuwa alert o podanym ID (tylko właściciela)."""
    alert = db.query(Alert).filter(
        Alert.id == alert_id,
        Alert.user_id == current_user.id
    ).first()

    if not alert:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Alert nie istnieje lub nie masz do niego dostępu."
        )

    db.delete(alert)
    db.commit()
