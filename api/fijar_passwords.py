from werkzeug.security import generate_password_hash
from app import app
from models import db, Usuario

with app.app_context():
    nuevo_hash = generate_password_hash("123456")
    n = (Usuario.query
         .filter_by(password_hash="hash_de_prueba")
         .update({"password_hash": nuevo_hash}))
    db.session.commit()
    print(f"{n} usuarios actualizados. Contraseña: 123456")
