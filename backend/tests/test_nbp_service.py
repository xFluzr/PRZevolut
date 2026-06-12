"""Testy serwisu NBP — mockujemy zewnętrzne API NBP."""

import datetime
from unittest.mock import AsyncMock, MagicMock, patch

import httpx
import pytest

from app.services.alert_engine import _check_threshold_crossed
from app.services.nbp_client import NbpRate, fetch_nbp_rates

MOCK_NBP_RESPONSE = [
    {
        "table": "A",
        "no": "113/A/NBP/2026",
        "effectiveDate": "2026-06-12",
        "rates": [
            {"currency": "dolar amerykański", "code": "USD", "mid": 3.9821},
            {"currency": "euro", "code": "EUR", "mid": 4.3245},
            {"currency": "funt szterling", "code": "GBP", "mid": 5.1190},
            {"currency": "frank szwajcarski", "code": "CHF", "mid": 4.4567},
        ],
    }
]


class TestNbpClient:
    @pytest.mark.asyncio
    async def test_fetch_nbp_rates_success(self):
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = MOCK_NBP_RESPONSE
        mock_response.raise_for_status = MagicMock()

        with patch("app.services.nbp_client.httpx.AsyncClient") as MockClient:
            mock_client_instance = AsyncMock()
            mock_client_instance.get.return_value = mock_response
            mock_client_instance.__aenter__ = AsyncMock(return_value=mock_client_instance)
            mock_client_instance.__aexit__ = AsyncMock(return_value=False)
            MockClient.return_value = mock_client_instance

            rates = await fetch_nbp_rates()

        assert len(rates) == 4
        eur = next(r for r in rates if r.code == "EUR")
        assert eur.rate_to_pln == 4.3245
        assert eur.name == "euro"
        assert isinstance(eur, NbpRate)

    @pytest.mark.asyncio
    async def test_fetch_nbp_rates_http_error_returns_empty(self):
        with patch("app.services.nbp_client.httpx.AsyncClient") as MockClient:
            mock_client_instance = AsyncMock()
            mock_client_instance.get.side_effect = httpx.HTTPError("503")
            mock_client_instance.__aenter__ = AsyncMock(return_value=mock_client_instance)
            mock_client_instance.__aexit__ = AsyncMock(return_value=False)
            MockClient.return_value = mock_client_instance

            rates = await fetch_nbp_rates()

        assert rates == []

    @pytest.mark.asyncio
    async def test_fetch_nbp_rates_timeout_returns_empty(self):
        with patch("app.services.nbp_client.httpx.AsyncClient") as MockClient:
            mock_client_instance = AsyncMock()
            mock_client_instance.get.side_effect = httpx.ReadTimeout("Connection timed out")
            mock_client_instance.__aenter__ = AsyncMock(return_value=mock_client_instance)
            mock_client_instance.__aexit__ = AsyncMock(return_value=False)
            MockClient.return_value = mock_client_instance

            rates = await fetch_nbp_rates()

        assert rates == []


class TestAlertEngine:
    def test_threshold_above_triggered(self):
        result = _check_threshold_crossed(
            current_rate=4.50,
            threshold=4.40,
            direction="above",
            last_triggered_at=None,
        )
        assert result is True

    def test_threshold_above_not_triggered(self):
        result = _check_threshold_crossed(
            current_rate=4.30,
            threshold=4.40,
            direction="above",
            last_triggered_at=None,
        )
        assert result is False

    def test_threshold_below_triggered(self):
        result = _check_threshold_crossed(
            current_rate=4.20,
            threshold=4.30,
            direction="below",
            last_triggered_at=None,
        )
        assert result is True

    def test_cooldown_prevents_retrigger(self):
        recent = datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(hours=1)
        result = _check_threshold_crossed(
            current_rate=4.50,
            threshold=4.40,
            direction="above",
            last_triggered_at=recent,
        )
        assert result is False

    def test_cooldown_expired_allows_retrigger(self):
        old = datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(hours=7)
        result = _check_threshold_crossed(
            current_rate=4.50,
            threshold=4.40,
            direction="above",
            last_triggered_at=old,
        )
        assert result is True

    def test_invalid_direction_returns_false(self):
        result = _check_threshold_crossed(
            current_rate=4.50,
            threshold=4.40,
            direction="invalid",
            last_triggered_at=None,
        )
        assert result is False
