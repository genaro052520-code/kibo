import os
from datetime import timedelta
from dotenv import load_dotenv

load_dotenv()


def _requerida(nombre):
    valor = os.environ.get(nombre)
    if not valor:
        raise RuntimeError(f"Falta la variable de entorno {nombre} (revisa tu archivo .env)")
    return valor


class Config:
    SQLALCHEMY_DATABASE_URI = _requerida("DATABASE_URL")
    SQLALCHEMY_TRACK_MODIFICATIONS = False
    # Evita errores por conexiones que MariaDB cierra tras un rato inactivo
    SQLALCHEMY_ENGINE_OPTIONS = {"pool_pre_ping": True, "pool_recycle": 280}

    JWT_SECRET_KEY = _requerida("JWT_SECRET_KEY")
    JWT_ACCESS_TOKEN_EXPIRES = timedelta(hours=12)
