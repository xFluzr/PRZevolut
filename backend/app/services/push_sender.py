"""Push sender — wysyłanie powiadomień FCM przez firebase-admin."""

import logging
import os

logger = logging.getLogger(__name__)

# Inicjalizacja firebase-admin (leniwa — tylko jeśli jest plik credentials)
_firebase_initialized = False


def _init_firebase() -> bool:
    """Inicjalizuje firebase-admin jeśli możliwe. Zwraca True jeśli sukces."""
    global _firebase_initialized
    if _firebase_initialized:
        return True

    credentials_path = os.getenv("FIREBASE_CREDENTIALS_PATH", "firebase-credentials.json")
    if not os.path.exists(credentials_path):
        logger.warning(
            "Plik credentials Firebase nie istnieje (%s) — push notifications wyłączone.",
            credentials_path,
        )
        return False

    try:
        import firebase_admin
        from firebase_admin import credentials

        cred = credentials.Certificate(credentials_path)
        firebase_admin.initialize_app(cred)
        _firebase_initialized = True
        logger.info("Firebase Admin SDK zainicjalizowany.")
        return True
    except Exception as exc:
        logger.error("Błąd inicjalizacji Firebase: %s", exc)
        return False


async def send_push_notification(
    token: str,
    title: str,
    body: str,
    data: dict[str, str] | None = None,
) -> bool:
    """
    Wysyła powiadomienie push przez FCM na podany token urządzenia.
    Zwraca True jeśli sukces, False w razie błędu.
    """
    if not _init_firebase():
        logger.debug("Firebase nieaktywny — symulacja push: title=%s body=%s", title, body)
        return False

    try:
        from firebase_admin import messaging

        message = messaging.Message(
            notification=messaging.Notification(title=title, body=body),
            data=data or {},
            token=token,
            android=messaging.AndroidConfig(
                priority="high",
                notification=messaging.AndroidNotification(
                    channel_id="przevolut_alerts",
                    icon="ic_notification",
                ),
            ),
        )
        response = messaging.send(message)
        logger.info("Push wysłany pomyślnie: %s", response)
        return True
    except Exception as exc:
        logger.error("Błąd wysyłki FCM: %s", exc)
        return False
