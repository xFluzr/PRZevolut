"""
Testy integracyjne — kursy walut.
Testuje endpointy: GET /rates i GET /rates/{currency}
"""
import pytest
from fastapi.testclient import TestClient
from datetime import datetime, timezone
from app.main import app
from tests.conftest import TestingSessionLocal
from app.db.models import ExchangeRate

client = TestClient(app)


@pytest.fixture(autouse=True)
def seed_rates(reset_db):
    """Dodaje testowe kursy walut do DB przed każdym testem w tym module."""
    db = TestingSessionLocal()
    now = datetime.now(timezone.utc)
    for currency, rate in [("EUR", 4.25), ("USD", 3.95), ("GBP", 5.10)]:
        db.add(ExchangeRate(
            currency=currency, rate=rate, mid=rate,
            fetched_at=now, effective_date="2026-05-04"
        ))
    db.commit()
    db.close()


class TestRates:
    def test_get_all_rates(self):
        """Pobranie wszystkich kursów — oczekiwana lista z minimum 1 rekordem."""
        response = client.get("/rates")
        assert response.status_code == 200
        data = response.json()
        assert len(data) >= 1
        assert "currency" in data[0]
        assert "mid" in data[0]

    def test_get_rate_eur(self):
        """Pobranie kursu EUR — oczekiwany prawidłowy rekord."""
        response = client.get("/rates/EUR")
        assert response.status_code == 200
        data = response.json()
        assert data["currency"] == "EUR"
        assert data["mid"] == pytest.approx(4.25)

    def test_get_rate_unsupported_currency(self):
        """Nieobsługiwana waluta (np. BTC) — oczekiwany błąd 400."""
        response = client.get("/rates/BTC")
        assert response.status_code == 400

    def test_get_rate_case_insensitive(self):
        """Kurs waluty podanej małymi literami — oczekiwany sukces."""
        response = client.get("/rates/eur")
        assert response.status_code == 200
