# syntax=docker/dockerfile:1

# ─── Stage 1: Builder ────────────────────────────────────────────────────────
FROM python:3.12-slim AS builder

WORKDIR /app

# Instalacja zależności systemowych potrzebnych do kompilacji
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    libpq-dev \
    && rm -rf /var/lib/apt/lists/*

# Kopiowanie pliku requirements i instalacja w wirtualnym środowisku
COPY requirements.txt .
RUN pip install --upgrade pip && \
    pip install --no-cache-dir --prefix=/install -r requirements.txt

# ─── Stage 2: Production ─────────────────────────────────────────────────────
FROM python:3.12-slim AS production

WORKDIR /app

# Kopiowanie zainstalowanych pakietów z buildera
COPY --from=builder /install /usr/local

# Kopiowanie kodu aplikacji
COPY . .

# Użytkownik nie-root (bezpieczeństwo)
RUN useradd --create-home --shell /bin/bash appuser && \
    chown -R appuser:appuser /app
USER appuser

# Zmienne środowiskowe
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PORT=8000

EXPOSE 8000

# Uruchomienie migracji bazy danych, a po ich zakończeniu start serwera Uvicorn
CMD ["sh", "-c", "alembic upgrade head && uvicorn app.main:app --host 0.0.0.0 --port 8000 --workers 2"]
