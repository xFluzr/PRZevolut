"""
Wspólna konfiguracja testów pytest.
Zapewnia izolowaną bazę SQLite dla każdego pliku testów.
"""
import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from app.db.database import Base, get_db
from app.main import app

TEST_DATABASE_URL = "sqlite:///./test_shared.db"

engine = create_engine(TEST_DATABASE_URL, connect_args={"check_same_thread": False})
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


def override_get_db():
    db = TestingSessionLocal()
    try:
        yield db
    finally:
        db.close()


app.dependency_overrides[get_db] = override_get_db


@pytest.fixture(scope="function", autouse=True)
def reset_db():
    """Tworzy świeże tabele przed każdym testem i niszczy je po nim."""
    Base.metadata.create_all(bind=engine)
    yield
    Base.metadata.drop_all(bind=engine)


@pytest.fixture(scope="session")
def client():
    return TestClient(app)
