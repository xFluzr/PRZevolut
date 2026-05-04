"""
Testy integracyjne — alerty walutowe.
Testuje endpointy: GET/POST /alerts i DELETE /alerts/{id}
"""
import pytest
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

USER = {"email": "alertuser@example.com", "password": "Password123!"}
ALERT_PAYLOAD = {"currency": "EUR", "direction": "below", "target_rate": 4.20}


def get_auth_token() -> str:
    client.post("/auth/register", json=USER)
    response = client.post("/auth/login", json=USER)
    return response.json()["access_token"]


class TestAlerts:
    def test_create_alert_authenticated(self):
        """Tworzenie alertu jako zalogowany użytkownik — oczekiwany sukces 201."""
        token = get_auth_token()
        response = client.post(
            "/alerts",
            json=ALERT_PAYLOAD,
            headers={"Authorization": f"Bearer {token}"}
        )
        assert response.status_code == 201
        data = response.json()
        assert data["currency"] == "EUR"
        assert data["target_rate"] == pytest.approx(4.20)

    def test_create_alert_unauthenticated(self):
        """Tworzenie alertu bez tokenu — oczekiwany błąd 401/403."""
        response = client.post("/alerts", json=ALERT_PAYLOAD)
        assert response.status_code in (401, 403)

    def test_list_alerts(self):
        """Lista alertów zalogowanego użytkownika — oczekiwana niepusta lista po dodaniu."""
        token = get_auth_token()
        headers = {"Authorization": f"Bearer {token}"}
        client.post("/alerts", json=ALERT_PAYLOAD, headers=headers)
        response = client.get("/alerts", headers=headers)
        assert response.status_code == 200
        assert len(response.json()) == 1

    def test_delete_alert(self):
        """Usunięcie alertu — oczekiwany status 204 i brak alertu po usunięciu."""
        token = get_auth_token()
        headers = {"Authorization": f"Bearer {token}"}
        create_response = client.post("/alerts", json=ALERT_PAYLOAD, headers=headers)
        alert_id = create_response.json()["id"]

        delete_response = client.delete(f"/alerts/{alert_id}", headers=headers)
        assert delete_response.status_code == 204

        list_response = client.get("/alerts", headers=headers)
        assert len(list_response.json()) == 0

    def test_delete_other_users_alert(self):
        """Próba usunięcia cudzego alertu — oczekiwany błąd 404."""
        token1 = get_auth_token()
        create_response = client.post(
            "/alerts", json=ALERT_PAYLOAD,
            headers={"Authorization": f"Bearer {token1}"}
        )
        alert_id = create_response.json()["id"]

        other_user = {"email": "other@example.com", "password": "pass123!"}
        client.post("/auth/register", json=other_user)
        login2 = client.post("/auth/login", json=other_user)
        token2 = login2.json()["access_token"]

        response = client.delete(f"/alerts/{alert_id}", headers={"Authorization": f"Bearer {token2}"})
        assert response.status_code == 404
