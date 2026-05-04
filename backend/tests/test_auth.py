"""
Testy integracyjne — autoryzacja.
Testuje endpointy: POST /auth/register i POST /auth/login
"""
import pytest
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

VALID_USER = {"email": "test@example.com", "password": "SecurePassword123"}


class TestRegister:
    def test_register_success(self):
        """Rejestracja nowego użytkownika — oczekiwany sukces 201."""
        response = client.post("/auth/register", json=VALID_USER)
        assert response.status_code == 201
        data = response.json()
        assert data["email"] == VALID_USER["email"]
        assert "id" in data
        assert "hashed_password" not in data  # hasło nie może być w odpowiedzi

    def test_register_duplicate_email(self):
        """Próba rejestracji z tym samym e-mailem — oczekiwany błąd 409."""
        client.post("/auth/register", json=VALID_USER)
        response = client.post("/auth/register", json=VALID_USER)
        assert response.status_code == 409

    def test_register_invalid_email(self):
        """Nieprawidłowy format e-mail — oczekiwany błąd 422."""
        response = client.post("/auth/register", json={"email": "notanemail", "password": "pass"})
        assert response.status_code == 422


class TestLogin:
    def test_login_success(self):
        """Logowanie prawidłowymi danymi — oczekiwany token JWT."""
        client.post("/auth/register", json=VALID_USER)
        response = client.post("/auth/login", json=VALID_USER)
        assert response.status_code == 200
        data = response.json()
        assert "access_token" in data
        assert "refresh_token" in data
        assert data["token_type"] == "bearer"

    def test_login_wrong_password(self):
        """Logowanie błędnym hasłem — oczekiwany błąd 401."""
        client.post("/auth/register", json=VALID_USER)
        response = client.post("/auth/login", json={**VALID_USER, "password": "WrongPassword"})
        assert response.status_code == 401

    def test_login_nonexistent_user(self):
        """Logowanie na nieistniejące konto — oczekiwany błąd 401."""
        response = client.post("/auth/login", json={"email": "ghost@example.com", "password": "pass"})
        assert response.status_code == 401
