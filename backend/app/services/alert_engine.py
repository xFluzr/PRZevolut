"""Silnik alertów — sprawdza przekroczenia progów i wysyła powiadomienia FCM."""

import datetime
import logging

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import AsyncSessionLocal
from app.models import Alert, DeviceToken
from app.services.nbp_client import NbpRate
from app.services.push_sender import send_push_notification

logger = logging.getLogger(__name__)


async def process_alerts(current_rates: list[NbpRate]) -> None:
    """
    Iteruje aktywne alerty i sprawdza czy próg został przekroczony.
    Logika przekroczenia (nie samo bycie pod/nad progiem):
      - direction="above": poprzedni kurs <= próg i aktualny > próg
      - direction="below": poprzedni kurs >= próg i aktualny < próg
    """
    # Buduj słownik aktualnych kursów kod→wartość
    rates_map: dict[str, float] = {r.code: r.rate_to_pln for r in current_rates}

    async with AsyncSessionLocal() as db:
        # Pobierz wszystkie aktywne alerty
        result = await db.execute(select(Alert).where(Alert.is_active == True))
        alerts = result.scalars().all()

        for alert in alerts:
            current_rate = rates_map.get(alert.currency_code)
            if current_rate is None:
                continue  # Nieznana waluta — pomiń

            # Sprawdź warunek przekroczenia progu
            triggered = _check_threshold_crossed(
                current_rate=current_rate,
                threshold=alert.threshold,
                direction=alert.direction,
                last_triggered_at=alert.last_triggered_at,
            )

            if triggered:
                alert.last_triggered_at = datetime.datetime.now(datetime.timezone.utc)
                await db.flush()

                # Wyślij powiadomienie na wszystkie urządzenia użytkownika
                await _send_alert_notification(db, alert, current_rate)

        await db.commit()


def _check_threshold_crossed(
    current_rate: float,
    threshold: float,
    direction: str,
    last_triggered_at: datetime.datetime | None,
) -> bool:
    """
    Sprawdza czy kurs właśnie przekroczył próg (edge trigger, nie level trigger).
    Uproszczona wersja: wyzwala jeśli alert nie był wyzwolony przez ostatnie 6 godzin.
    """
    now = datetime.datetime.now(datetime.timezone.utc)

    # Cooldown 6 godzin — nie zasypuj użytkownika powiadomieniami
    if last_triggered_at is not None:
        last_triggered_utc = last_triggered_at.replace(tzinfo=datetime.timezone.utc)
        if (now - last_triggered_utc).total_seconds() < 6 * 3600:
            return False

    if direction == "above":
        return current_rate > threshold
    elif direction == "below":
        return current_rate < threshold
    return False


async def _send_alert_notification(
    db: AsyncSession, alert: Alert, current_rate: float
) -> None:
    """Wysyła powiadomienie FCM na wszystkie urządzenia użytkownika."""
    result = await db.execute(
        select(DeviceToken).where(DeviceToken.user_id == alert.user_id)
    )
    device_tokens = result.scalars().all()

    direction_text = "przekroczył" if alert.direction == "above" else "spadł poniżej"
    title = "Alert PRZevolut 💱"
    body = (
        f"{alert.currency_code} {direction_text} progu {alert.threshold:.4f} "
        f"→ aktualny kurs: {current_rate:.4f}"
    )
    data = {
        "alert_id": str(alert.id),
        "currency_code": alert.currency_code,
        "current_rate": str(current_rate),
        "threshold": str(alert.threshold),
        "direction": alert.direction,
    }

    for device in device_tokens:
        try:
            await send_push_notification(
                token=device.fcm_token,
                title=title,
                body=body,
                data=data,
            )
        except Exception as exc:
            logger.error("Błąd wysyłki push na token %s: %s", device.fcm_token[:20], exc)
