"""Testy integracyjne backendu PRZevolut."""

import datetime

import pytest
import pytest_asyncio
from httpx import ASGITransport, AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine

from app.database import Base, get_db
from app.main import app
from app.models import Rate

TEST_DATABASE_URL = "sqlite+aiosqlite:///:memory:"

test_engine = create_async_engine(TEST_DATABASE_URL, echo=False)
TestSessionLocal = async_sessionmaker(
    bind=test_engine, class_=AsyncSession, expire_on_commit=False
)


@pytest_asyncio.fixture(scope="function", autouse=True)
async def setup_database():
    async with test_engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    yield
    async with test_engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)


@pytest_asyncio.fixture
async def db_session():
    async with TestSessionLocal() as session:
        yield session


@pytest_asyncio.fixture
async def client(db_session):
    async def override_get_db():
        yield db_session

    app.dependency_overrides[get_db] = override_get_db

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        yield ac

    app.dependency_overrides.clear()


@pytest.mark.asyncio
async def test_register_and_login_returns_jwt(client: AsyncClient):
    register_response = await client.post(
        "/auth/register",
        json={"email": "test@example.com", "password": "StrongPass1"},
    )
    assert register_response.status_code == 201
    data = register_response.json()
    assert "access_token" in data
    assert "refresh_token" in data
    assert data["token_type"] == "bearer"

    login_response = await client.post(
        "/auth/login",
        json={"email": "test@example.com", "password": "StrongPass1"},
    )
    assert login_response.status_code == 200
    login_data = login_response.json()
    assert "access_token" in login_data
    assert "refresh_token" in login_data


@pytest.mark.asyncio
async def test_rates_unauthorized_and_authorized(client: AsyncClient, db_session: AsyncSession):
    response = await client.get("/rates")
    assert response.status_code == 401

    await client.post(
        "/auth/register",
        json={"email": "ratesuser@example.com", "password": "StrongPass1"},
    )
    login_resp = await client.post(
        "/auth/login",
        json={"email": "ratesuser@example.com", "password": "StrongPass1"},
    )
    token = login_resp.json()["access_token"]

    rate = Rate(
        code="EUR",
        name="euro",
        rate_to_pln=4.31,
        fetched_at=datetime.datetime.now(datetime.timezone.utc),
    )
    db_session.add(rate)
    await db_session.commit()

    response_auth = await client.get(
        "/rates",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert response_auth.status_code == 200
    data = response_auth.json()
    assert "rates" in data
    assert len(data["rates"]) == 1
    assert data["rates"][0]["code"] == "EUR"


@pytest.mark.asyncio
async def test_alert_crud(client: AsyncClient):
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

    create_resp = await client.post(
        "/alerts",
        json={"currency_code": "EUR", "direction": "above", "threshold": 4.40},
        headers=headers,
    )
    assert create_resp.status_code == 201
    alert_id = create_resp.json()["id"]
    assert create_resp.json()["currency_code"] == "EUR"
    assert create_resp.json()["threshold"] == 4.40

    list_resp = await client.get("/alerts", headers=headers)
    assert list_resp.status_code == 200
    assert len(list_resp.json()) == 1
    assert list_resp.json()[0]["id"] == alert_id

    patch_resp = await client.patch(
        f"/alerts/{alert_id}",
        json={"threshold": 4.35},
        headers=headers,
    )
    assert patch_resp.status_code == 200
    assert patch_resp.json()["threshold"] == 4.35

    delete_resp = await client.delete(f"/alerts/{alert_id}", headers=headers)
    assert delete_resp.status_code == 204

    list_after_resp = await client.get("/alerts", headers=headers)
    assert list_after_resp.json() == []


@pytest.mark.asyncio
async def test_refresh_token_rotation(client: AsyncClient):
    register_resp = await client.post(
        "/auth/register",
        json={"email": "refreshuser@example.com", "password": "StrongPass1"},
    )
    refresh_token = register_resp.json()["refresh_token"]

    refresh_resp = await client.post(
        "/auth/refresh",
        json={"refresh_token": refresh_token},
    )
    assert refresh_resp.status_code == 200
    new_access_token = refresh_resp.json()["access_token"]
    assert new_access_token

    refresh_again_resp = await client.post(
        "/auth/refresh",
        json={"refresh_token": refresh_token},
    )
    assert refresh_again_resp.status_code == 401


@pytest.mark.asyncio
async def test_register_duplicate_email_returns_409(client: AsyncClient):
    payload = {"email": "dup@example.com", "password": "StrongPass1"}
    first = await client.post("/auth/register", json=payload)
    assert first.status_code == 201

    second = await client.post("/auth/register", json=payload)
    assert second.status_code == 409
