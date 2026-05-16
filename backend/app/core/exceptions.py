"""Wyjątki HTTP — wspólne klasy błędów."""

from fastapi import HTTPException, status


class CredentialsException(HTTPException):
    def __init__(self, detail: str = "Nieprawidłowe dane uwierzytelniające."):
        super().__init__(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail=detail,
            headers={"WWW-Authenticate": "Bearer"},
        )


class ForbiddenException(HTTPException):
    def __init__(self, detail: str = "Brak uprawnień."):
        super().__init__(status_code=status.HTTP_403_FORBIDDEN, detail=detail)


class NotFoundException(HTTPException):
    def __init__(self, detail: str = "Zasób nie istnieje."):
        super().__init__(status_code=status.HTTP_404_NOT_FOUND, detail=detail)


class ConflictException(HTTPException):
    def __init__(self, detail: str = "Konflikt — zasób już istnieje."):
        super().__init__(status_code=status.HTTP_409_CONFLICT, detail=detail)
