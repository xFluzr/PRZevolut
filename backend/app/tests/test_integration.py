"""Testy integracyjne backendu PRZevolut — minimum 5 testów."""

import datetime
import pytest
import pytest_asyncio
from httpx import AsyncClient, ASGITransport
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine

from app.database import Base, get_db
from app.main import app
from app.models import Alert, Rate

# Baza testowa w pamięci (SQLite async)
TEST_DATABASE_URL = "sqlite+aiosqlite:///:memory:"

test_engine = create_async_engine(TEST_DATABASE_URL, echo=False)
TestSessionLocal = async_sessionmaker(
    bind=test_engine, class_=AsyncSession, expire_on_commit=False
)


@pytest_asyncio.fixture(scope="function", autouse=True)
async def setup_database():
    """Tworzy tabele przed każdym testem i usuwa po."""
    async with test_engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    yield
    async with test_engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)


@pytest_asyncio.fixture
async def db_session():
    """Sesja testowej bazy danych."""
    async with TestSessionLocal() as session:
        yield session


@pytest_asyncio.fixture
async def client(db_session):
    """Klient HTTP z nadpisaną dependency get_db."""
    async def override_get_db():
        yield db_session

    app.dependency_overrides[get_db] = override_get_db

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        yield ac

    app.dependency_overrides.clear()


# ─── Test 1: Rejestracja + login zwraca JWT ───────────────────────────────────

@pytest.mark.asyncio
async def test_register_and_login_returns_jwt(client: AsyncClient):
    """Test 1: Rejestracja i login zwracają access_token i refresh_token."""
    # Rejestracja
    register_response = await client.post(
        "/auth/register",
        json={"email": "test@example.com", "password": "StrongPass1"},
    )
    assert register_response.status_code == 201
    data = register_response.json()
    assert "access_token" in data
    assert "refresh_token" in data
    assert data["token_type"] == "bearer"

    # Login
    login_response = await client.post(
        "/auth/login",
        json={"email": "test@example.com", "password": "StrongPass1"},
    )
    assert login_response.status_code == 200
    login_data = login_response.json()
    assert "access_token" in login_data
    assert "refresh_token" in login_data


# ─── Test 2: /rates 401 bez tokena, 200 z tokenem ────────────────────────────

@pytest.mark.asyncio
async def test_rates_unauthorized_and_authorized(client: AsyncClient, db_session: AsyncSession):
    """Test 2: /rates zwraca 401 bez tokena i 200 z tokenem."""
    # Bez tokena
    response = await client.get("/rates")
    assert response.status_code == 401

    # Rejestracja i login
    await client.post(
        "/auth/register",
        json={"email": "ratesuser@example.com", "password": "StrongPass1"},
    )
    login_resp = await client.post(
        "/auth/login",
        json={"email": "ratesuser@example.com", "password": "StrongPass1"},
    )
    token = login_resp.json()["access_token"]

    # Dodaj przykładowy kurs do testowej bazy
    rate = Rate(
        code="EUR",
        name="euro",
        rate_to_pln=4.31,
        fetched_at=datetime.datetime.now(datetime.timezone.utc),
    )
    db_session.add(rate)
    await db_session.commit()

    # Z tokenem
    response_auth = await client.get(
        "/rates",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert response_auth.status_code == 200
    data = response_auth.json()
    assert "rates" in data
    assert len(data["rates"]) == 1
    assert data["rates"][0]["code"] == "EUR"


# ─── Test 3: CRUD alertu (utwórz → odczytaj → edytuj → usuń) ────────────────

@pytest.mark.asyncio
async def test_alert_crud(client: AsyncClient):
    """Test 3: Pełny cykl CRUD alertu walutowego."""
    # Setup — rejestracja i token
    await client.post(
        "/auth/register",
        json={"email": "alertuser@example.com", "password": "StrongPass1"},
    )
    login_resp = await client.post(
        "/auth/login",
        json={"email": "alertuser@example.com", "password": "StrongPass1"},
    )
    token = login_resp.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    # Utwórz alert
    create_resp = await client.post(
        "/alerts",
        json={"currency_code": "EUR", "direction": "above", "threshold": 4.40},
        headers=headers,
    )
    assert create_resp.status_code == 201
    alert_id = create_resp.json()["id"]
    assert create_resp.json()["currency_code"] == "EUR"
    assert create_resp.json()["threshold"] == 4.40

    # Odczytaj listę
    list_resp = await client.get("/alerts", headers=headers)
    assert list_resp.status_code == 200
    assert len(list_resp.json()) == 1
    assert list_resp.json()[0]["id"] == alert_id

    # Edytuj alert
    patch_resp = await client.patch(
        f"/alerts/{alert_id}",
        json={"threshold": 4.35},
        headers=headers,
    )
    assert patch_resp.status_code == 200
    assert patch_resp.json()["threshold"] == 4.35

    # Usuń alert
    delete_resp = await client.delete(f"/alerts/{alert_id}", headers=headers)
    assert delete_resp.status_code == 204

    # Weryfikuj że lista jest pusta
    list_after_resp = await client.get("/alerts", headers=headers)
    assert list_after_resp.json() == []


# ─── Test 4: Silnik alertów wyzwala się gdy próg przekroczony ─────────────────

@pytest.mark.asyncio
async def test_alert_engine_triggers_on_threshold(db_session: AsyncSession):
    """Test 4: Silnik alertów wykrywa przekroczenie progu."""
    from app.services.alert_engine import _check_threshold_crossed

    # above — kurs przekracza próg
    assert _check_threshold_crossed(
        current_rate=4.42,
        threshold=4.40,
        direction="above",
        last_triggered_at=None,
    ) is True

    # above — kurs poniżej progu
    assert _check_threshold_crossed(
        current_rate=4.38,
        threshold=4.40,
        direction="above",
        last_triggered_at=None,
    ) is False

    # below — kurs spada poniżej progu
    assert _check_threshold_crossed(
        current_rate=3.80,
        threshold=4.00,
        direction="below",
        last_triggered_at=None,
    ) is True

    # below — kurs powyżej progu
    assert _check_threshold_crossed(
        current_rate=4.20,
        threshold=4.00,
        direction="below",
        last_triggered_at=None,
    ) is False

    # Cooldown — alert wyzwolony przed <6h nie wyzwala ponownie
    recent = datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(hours=1)
    assert _check_threshold_crossed(
        current_rate=4.42,
        threshold=4.40,
        direction="above",
        last_triggered_at=recent,
    ) is False


# ─── Test 5: Refresh token rotacja ───────────────────────────────────────────

@pytest.mark.asyncio
async def test_refresh_token_rotation(client: AsyncClient):
    """Test 5: Refresh token rotacja — stary token unieważniony po użyciu."""
    # Rejestracja
    register_resp = await client.post(
        "/auth/register",
        json={"email": "refreshuser@example.com", "password": "StrongPass1"},
    )
    refresh_token = register_resp.json()["refresh_token"]

    # Użyj refresh tokena
    refresh_resp = await client.post(
        "/auth/refresh",
        json={"refresh_token": refresh_token},
    )
    assert refresh_resp.status_code == 200
    new_access_token = refresh_resp.json()["access_token"]
    assert new_access_token  # Nowy token nie jest pusty

    # Ponowne użycie tego samego refresh tokena powinno zwrócić błąd
    # (token jest oznaczony jako revoked po rotacji)
    refresh_again_resp = await client.post(
        "/auth/refresh",
        json={"refresh_token": refresh_token},
    )
    assert refresh_again_resp.status_code == 401


# ─── Test 6: Rejestracja duplikatu e-mail → 409 ──────────────────────────────

@pytest.mark.asyncio
async def test_register_duplicate_email_returns_409(client: AsyncClient):
    """Test 6: Próba rejestracji z już używanym e-mailem zwraca 409 Conflict."""
    payload = {"email": "dup@example.com", "password": "StrongPass1"}
    first = await client.post("/auth/register", json=payload)
    assert first.status_code == 201

    second = await client.post("/auth/register", json=payload)
    assert second.status_code == 409
