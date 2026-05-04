FROM python:3.12-slim

# Ustaw katalog roboczy
WORKDIR /app

# Zainstaluj zależności systemowe
RUN apt-get update && apt-get install -y \
    gcc \
    libpq-dev \
    && rm -rf /var/lib/apt/lists/*

# Kopiuj i zainstaluj zależności Pythona
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Kopiuj kod aplikacji
COPY . .

# Utwórz niepriwilegowanego użytkownika
RUN adduser --disabled-password --gecos "" appuser && chown -R appuser /app
USER appuser

# Ekspozycja portu
EXPOSE 8000

# Start aplikacji
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
