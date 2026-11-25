from flask_sqlalchemy import SQLAlchemy
from datetime import datetime, date

db = SQLAlchemy()


class Plan(db.Model):
    __tablename__ = "planes"
    id_plan = db.Column(db.Integer, primary_key=True)
    nombre = db.Column(db.String(20), unique=True, nullable=False)
    max_miembros_hogar = db.Column(db.Integer, nullable=False)
    max_items_inventario = db.Column(db.Integer)
    precio_mensual = db.Column(db.Numeric(6, 2), nullable=False, default=0)


class Usuario(db.Model):
    __tablename__ = "usuarios"
    id_usuario = db.Column(db.Integer, primary_key=True)
    nombre = db.Column(db.String(100), nullable=False)
    email = db.Column(db.String(150), unique=True, nullable=False)
    password_hash = db.Column(db.String(255), nullable=False)
    id_plan = db.Column(db.Integer, db.ForeignKey("planes.id_plan"), nullable=False)
    fecha_registro = db.Column(db.DateTime, default=datetime.utcnow)
    plan = db.relationship("Plan")


class Hogar(db.Model):
    __tablename__ = "hogares"
    id_hogar = db.Column(db.Integer, primary_key=True)
    nombre = db.Column(db.String(100), nullable=False)
    fecha_creacion = db.Column(db.DateTime, default=datetime.utcnow)


class HogarMiembro(db.Model):
    __tablename__ = "hogar_miembros"
    id_hogar = db.Column(db.Integer, db.ForeignKey("hogares.id_hogar"), primary_key=True)
    id_usuario = db.Column(db.Integer, db.ForeignKey("usuarios.id_usuario"), primary_key=True)
    rol = db.Column(db.Enum("admin", "miembro"), nullable=False, default="miembro")
    fecha_union = db.Column(db.DateTime, default=datetime.utcnow)


class Categoria(db.Model):
    __tablename__ = "categorias"
    id_categoria = db.Column(db.Integer, primary_key=True)
    nombre = db.Column(db.String(60), unique=True, nullable=False)


class UnidadMedida(db.Model):
    __tablename__ = "unidades_medida"
    id_unidad = db.Column(db.Integer, primary_key=True)
    nombre = db.Column(db.String(30), unique=True, nullable=False)
    abreviatura = db.Column(db.String(10), nullable=False)


class Producto(db.Model):
    __tablename__ = "productos"
    id_producto = db.Column(db.Integer, primary_key=True)
    nombre = db.Column(db.String(120), nullable=False)
    id_categoria = db.Column(db.Integer, db.ForeignKey("categorias.id_categoria"), nullable=False)
    dias_vida_util = db.Column(db.Integer, nullable=False)
    id_hogar = db.Column(db.Integer, db.ForeignKey("hogares.id_hogar"), nullable=True)
    categoria = db.relationship("Categoria")


class Inventario(db.Model):
    __tablename__ = "inventario"
    id_item = db.Column(db.Integer, primary_key=True)
    id_hogar = db.Column(db.Integer, db.ForeignKey("hogares.id_hogar"), nullable=False)
    id_producto = db.Column(db.Integer, db.ForeignKey("productos.id_producto"), nullable=False)
    id_unidad = db.Column(db.Integer, db.ForeignKey("unidades_medida.id_unidad"), nullable=False)
    agregado_por = db.Column(db.Integer, db.ForeignKey("usuarios.id_usuario"))
    cantidad = db.Column(db.Numeric(8, 2), nullable=False, default=1)
    fecha_ingreso = db.Column(db.Date, nullable=False, default=date.today)
    fecha_caducidad = db.Column(db.Date, nullable=False)
    estado = db.Column(db.Enum("disponible", "consumido", "desechado"),
                       nullable=False, default="disponible")
    producto = db.relationship("Producto")
    unidad = db.relationship("UnidadMedida")

class Invitacion(db.Model):
    __tablename__ = "invitaciones"
    id_invitacion = db.Column(db.Integer, primary_key=True)
    id_hogar = db.Column(db.Integer, db.ForeignKey("hogares.id_hogar"), nullable=False)
    codigo = db.Column(db.String(12), unique=True, nullable=False)
    creado_por = db.Column(db.Integer, db.ForeignKey("usuarios.id_usuario"))
    fecha_creacion = db.Column(db.DateTime, default=datetime.utcnow)
    fecha_expiracion = db.Column(db.DateTime, nullable=False)

class Alias(db.Model):
    __tablename__ = "alias_productos"
    id_alias = db.Column(db.Integer, primary_key=True)
    id_producto = db.Column(db.Integer, db.ForeignKey("productos.id_producto"), nullable=False)
    alias = db.Column(db.String(120), nullable=False)
