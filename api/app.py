import secrets
import os
from datetime import date, datetime, timedelta
from flask import Flask, request, jsonify
from flask_jwt_extended import (JWTManager, create_access_token,
                                jwt_required, get_jwt_identity)
from werkzeug.security import generate_password_hash, check_password_hash
from sqlalchemy import or_
from flask_limiter import Limiter
from flask_limiter.util import get_remote_address

from config import Config
from models import (db, Plan, Usuario, Hogar, HogarMiembro,
                    Producto, UnidadMedida, Inventario, Invitacion, Categoria)

app = Flask(__name__)
app.config.from_object(Config)
db.init_app(app)
jwt = JWTManager(app)
limiter = Limiter(get_remote_address, app=app, default_limits=[], storage_uri="memory://")
DEBUG = os.environ.get("FLASK_DEBUG") == "1"


def detalle(e):
    """Registra el error completo en el log; al cliente solo le muestra el detalle en modo debug."""
    app.logger.exception("Error no controlado")
    return str(e) if DEBUG else "Error interno del servidor"

DIAS_VALIDEZ_INVITACION = 7
ALFABETO_CODIGO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"   # sin 0/O/1/I para evitar confusiones


# ====================== UTILIDADES ======================
def usuario_actual():
    return int(get_jwt_identity())


def es_miembro(id_hogar, id_usuario):
    return HogarMiembro.query.filter_by(
        id_hogar=id_hogar, id_usuario=id_usuario).first() is not None


def es_admin(id_hogar, id_usuario):
    return HogarMiembro.query.filter_by(
        id_hogar=id_hogar, id_usuario=id_usuario, rol="admin").first() is not None


def plan_del_hogar(id_hogar):
    """El plan del hogar es el del administrador."""
    admin = HogarMiembro.query.filter_by(id_hogar=id_hogar, rol="admin").first()
    if admin:
        return db.session.get(Usuario, admin.id_usuario).plan
    return Plan.query.filter_by(nombre="Free").first()


def miembros_en(id_hogar):
    return HogarMiembro.query.filter_by(id_hogar=id_hogar).count()


def items_en(id_hogar):
    return Inventario.query.filter_by(id_hogar=id_hogar, estado="disponible").count()


def hogar_lleno(id_hogar):
    return miembros_en(id_hogar) >= plan_del_hogar(id_hogar).max_miembros_hogar


def error_limite_miembros(id_hogar):
    plan = plan_del_hogar(id_hogar)
    return jsonify({
        "error": f"El hogar alcanzó el límite de {plan.max_miembros_hogar} miembros "
                 f"del plan {plan.nombre}",
        "codigo": "limite_plan"
    }), 403


def buscar_invitacion_valida(codigo):
    """Devuelve (invitacion, None) si sirve, o (None, respuesta_de_error)."""
    inv = Invitacion.query.filter_by(codigo=codigo).first()
    if not inv:
        return None, (jsonify({"error": "Código de invitación inválido"}), 404)
    if inv.fecha_expiracion < datetime.utcnow():
        return None, (jsonify({"error": "El código de invitación expiró"}), 400)
    return inv, None


# ====================== ERRORES DE TOKEN ======================
@jwt.unauthorized_loader
def token_faltante(motivo):
    return jsonify({"error": "Falta el token. Inicia sesión en /api/login"}), 401


@jwt.expired_token_loader
def token_expirado(header, payload):
    return jsonify({"error": "El token expiró. Inicia sesión de nuevo"}), 401


@jwt.invalid_token_loader
def token_invalido(motivo):
    return jsonify({"error": "Token inválido"}), 401


@app.route("/")
def inicio():
    return jsonify({"mensaje": "API Despensa Inteligente funcionando", "version": "1.2"}), 200


# ====================== REGISTRO (público) ======================
# Sin código de invitación: se crea un hogar nuevo (admin).
# Con "codigo_invitacion": se une a ese hogar como miembro.
@app.route("/api/usuarios", methods=["POST"])
def registrar_usuario():
    try:
        data = request.get_json(silent=True) or {}
        nombre = data.get("nombre")
        email = data.get("email")
        password = data.get("password")

        if not nombre or not email or not password:
            return jsonify({"error": "nombre, email y password son obligatorios"}), 400
        if len(password) < 6:
            return jsonify({"error": "La contraseña debe tener al menos 6 caracteres"}), 400
        if Usuario.query.filter_by(email=email).first():
            return jsonify({"error": "El email ya está registrado"}), 400

        plan_free = Plan.query.filter_by(nombre="Free").first()
        if not plan_free:
            return jsonify({"error": "Plan Free no existe en la BD"}), 404

        codigo = (data.get("codigo_invitacion") or "").strip().upper()
        if codigo:
            inv, error = buscar_invitacion_valida(codigo)
            if error:
                return error
            if hogar_lleno(inv.id_hogar):
                return error_limite_miembros(inv.id_hogar)
            id_hogar = inv.id_hogar
            rol = "miembro"
        else:
            hogar = Hogar(nombre=data.get("nombre_hogar") or f"Hogar de {nombre}")
            db.session.add(hogar)
            db.session.flush()
            id_hogar = hogar.id_hogar
            rol = "admin"

        usuario = Usuario(nombre=nombre, email=email,
                          password_hash=generate_password_hash(password),
                          id_plan=plan_free.id_plan)
        db.session.add(usuario)
        db.session.flush()

        db.session.add(HogarMiembro(id_hogar=id_hogar,
                                    id_usuario=usuario.id_usuario, rol=rol))
        db.session.commit()

        return jsonify({
            "mensaje": "Usuario registrado correctamente",
            "id_usuario": usuario.id_usuario,
            "id_hogar": id_hogar,
            "rol": rol,
            "plan": "Free",
            "access_token": create_access_token(identity=str(usuario.id_usuario))
        }), 201

    except Exception as e:
        db.session.rollback()
        return jsonify({"error": "Error interno", "detalle": detalle(e)}), 500


# ====================== LOGIN (público) ======================
@app.route("/api/login", methods=["POST"])
@limiter.limit("5 per minute")
def login():
    try:
        data = request.get_json(silent=True) or {}
        email = data.get("email")
        password = data.get("password")

        if not email or not password:
            return jsonify({"error": "email y password son obligatorios"}), 400

        usuario = Usuario.query.filter_by(email=email).first()
        if not usuario or not check_password_hash(usuario.password_hash, password):
            return jsonify({"error": "Credenciales incorrectas"}), 401

        membresias = HogarMiembro.query.filter_by(id_usuario=usuario.id_usuario).all()
        return jsonify({
            "access_token": create_access_token(identity=str(usuario.id_usuario)),
            "usuario": {
                "id_usuario": usuario.id_usuario,
                "nombre": usuario.nombre,
                "email": usuario.email,
                "plan": usuario.plan.nombre
            },
            "hogares": [{"id_hogar": m.id_hogar, "rol": m.rol} for m in membresias]
        }), 200

    except Exception as e:
        return jsonify({"error": "Error interno", "detalle": detalle(e)}), 500


# ====================== PLAN (protegido) ======================
@app.route("/api/plan", methods=["GET"])
@jwt_required()
def ver_plan():
    try:
        usuario = db.session.get(Usuario, usuario_actual())
        if not usuario:
            return jsonify({"error": "Usuario no encontrado"}), 404

        hogares = []
        for m in HogarMiembro.query.filter_by(id_usuario=usuario.id_usuario).all():
            plan_h = plan_del_hogar(m.id_hogar)
            hogares.append({
                "id_hogar": m.id_hogar,
                "rol": m.rol,
                "plan_del_hogar": plan_h.nombre,
                "miembros": {"usados": miembros_en(m.id_hogar),
                             "maximo": plan_h.max_miembros_hogar},
                "items": {"usados": items_en(m.id_hogar),
                          "maximo": plan_h.max_items_inventario}   # null = ilimitado
            })

        return jsonify({
            "plan": usuario.plan.nombre,
            "precio_mensual": float(usuario.plan.precio_mensual),
            "hogares": hogares
        }), 200

    except Exception as e:
        return jsonify({"error": "Error interno", "detalle": detalle(e)}), 500


# SIMULADO: en producción esto se activa después de verificar un pago real
@app.route("/api/plan/premium", methods=["POST"])
@jwt_required()
def mejorar_a_premium():
    try:
        usuario = db.session.get(Usuario, usuario_actual())
        if not usuario:
            return jsonify({"error": "Usuario no encontrado"}), 404
        if usuario.plan.nombre == "Premium":
            return jsonify({"error": "Ya tienes el plan Premium"}), 400

        premium = Plan.query.filter_by(nombre="Premium").first()
        if not premium:
            return jsonify({"error": "Plan Premium no existe en la BD"}), 404

        usuario.id_plan = premium.id_plan
        db.session.commit()
        return jsonify({"mensaje": "Ahora tienes el plan Premium", "plan": "Premium"}), 200

    except Exception as e:
        db.session.rollback()
        return jsonify({"error": "Error interno", "detalle": detalle(e)}), 500


# ====================== INVITACIONES (protegido) ======================
@app.route("/api/hogares/<int:id_hogar>/invitaciones", methods=["POST"])
@jwt_required()
def crear_invitacion(id_hogar):
    try:
        id_usuario = usuario_actual()
        if not db.session.get(Hogar, id_hogar):
            return jsonify({"error": "Hogar no encontrado"}), 404
        if not es_admin(id_hogar, id_usuario):
            return jsonify({"error": "Solo el administrador puede invitar"}), 403

        for _ in range(5):   # reintenta si el código ya existe
            codigo = "".join(secrets.choice(ALFABETO_CODIGO) for _ in range(8))
            if not Invitacion.query.filter_by(codigo=codigo).first():
                break
        else:
            return jsonify({"error": "No se pudo generar el código, intenta de nuevo"}), 500

        expira = datetime.utcnow() + timedelta(days=DIAS_VALIDEZ_INVITACION)
        db.session.add(Invitacion(id_hogar=id_hogar, codigo=codigo,
                                  creado_por=id_usuario, fecha_expiracion=expira))
        db.session.commit()

        return jsonify({
            "codigo": codigo,
            "expira": expira.isoformat() + "Z",
            "mensaje": f"Válido por {DIAS_VALIDEZ_INVITACION} días"
        }), 201

    except Exception as e:
        db.session.rollback()
        return jsonify({"error": "Error interno", "detalle": detalle(e)}), 500


# Un usuario que YA tiene cuenta se une a otro hogar con un código
@app.route("/api/hogares/unirse", methods=["POST"])
@jwt_required()
def unirse_a_hogar():
    try:
        id_usuario = usuario_actual()
        data = request.get_json(silent=True) or {}
        codigo = (data.get("codigo") or "").strip().upper()
        if not codigo:
            return jsonify({"error": "codigo es obligatorio"}), 400

        inv, error = buscar_invitacion_valida(codigo)
        if error:
            return error
        if es_miembro(inv.id_hogar, id_usuario):
            return jsonify({"error": "Ya perteneces a este hogar"}), 400
        if hogar_lleno(inv.id_hogar):
            return error_limite_miembros(inv.id_hogar)

        db.session.add(HogarMiembro(id_hogar=inv.id_hogar,
                                    id_usuario=id_usuario, rol="miembro"))
        db.session.commit()
        return jsonify({"mensaje": "Te uniste al hogar", "id_hogar": inv.id_hogar}), 201

    except Exception as e:
        db.session.rollback()
        return jsonify({"error": "Error interno", "detalle": detalle(e)}), 500


# ====================== INVENTARIO (protegido) ======================
@app.route("/api/hogares/<int:id_hogar>/inventario", methods=["POST"])
@jwt_required()
def agregar_alimento(id_hogar):
    try:
        id_usuario = usuario_actual()

        if not db.session.get(Hogar, id_hogar):
            return jsonify({"error": "Hogar no encontrado"}), 404
        if not es_miembro(id_hogar, id_usuario):
            return jsonify({"error": "No perteneces a este hogar"}), 403

        # ---- Límite del plan ----
        plan = plan_del_hogar(id_hogar)
        if (plan.max_items_inventario is not None
                and items_en(id_hogar) >= plan.max_items_inventario):
            return jsonify({
                "error": f"Alcanzaste el límite de {plan.max_items_inventario} alimentos "
                         f"del plan {plan.nombre}. Pasa a Premium para agregar más",
                "codigo": "limite_plan"
            }), 403

        data = request.get_json(silent=True) or {}
        id_producto = data.get("id_producto")
        id_unidad = data.get("id_unidad")
        if not id_producto or not id_unidad:
            return jsonify({"error": "id_producto e id_unidad son obligatorios"}), 400

        producto = db.session.get(Producto, id_producto)
        # Solo productos globales o personalizados de ESTE hogar
        if not producto or producto.id_hogar not in (None, id_hogar):
            return jsonify({"error": "Producto no encontrado"}), 404
        if not db.session.get(UnidadMedida, id_unidad):
            return jsonify({"error": "Unidad no encontrada"}), 404

        try:
            f_ingreso = (datetime.strptime(data["fecha_ingreso"], "%Y-%m-%d").date()
                         if data.get("fecha_ingreso") else date.today())
            f_caducidad = (datetime.strptime(data["fecha_caducidad"], "%Y-%m-%d").date()
                           if data.get("fecha_caducidad")
                           else f_ingreso + timedelta(days=producto.dias_vida_util))
        except ValueError:
            return jsonify({"error": "Formato de fecha inválido, usa YYYY-MM-DD"}), 400

        item = Inventario(id_hogar=id_hogar, id_producto=id_producto,
                          id_unidad=id_unidad, agregado_por=id_usuario,
                          cantidad=data.get("cantidad", 1),
                          fecha_ingreso=f_ingreso, fecha_caducidad=f_caducidad)
        db.session.add(item)
        db.session.commit()

        return jsonify({
            "mensaje": "Alimento agregado",
            "id_item": item.id_item,
            "producto": producto.nombre,
            "fecha_caducidad": f_caducidad.isoformat()
        }), 201

    except Exception as e:
        db.session.rollback()
        return jsonify({"error": "Error interno", "detalle": detalle(e)}), 500


@app.route("/api/hogares/<int:id_hogar>/inventario", methods=["GET"])
@jwt_required()
def consultar_inventario(id_hogar):
    try:
        if not db.session.get(Hogar, id_hogar):
            return jsonify({"error": "Hogar no encontrado"}), 404
        if not es_miembro(id_hogar, usuario_actual()):
            return jsonify({"error": "No perteneces a este hogar"}), 403

        items = (Inventario.query
                 .filter_by(id_hogar=id_hogar, estado="disponible")
                 .order_by(Inventario.fecha_caducidad.asc())
                 .all())

        hoy = date.today()
        resultado = [{
            "id_item": i.id_item,
            "producto": i.producto.nombre,
            "categoria": i.producto.categoria.nombre,
            "cantidad": float(i.cantidad),
            "unidad": i.unidad.abreviatura,
            "fecha_ingreso": i.fecha_ingreso.isoformat(),
            "fecha_caducidad": i.fecha_caducidad.isoformat(),
            "dias_restantes": (i.fecha_caducidad - hoy).days
        } for i in items]

        return jsonify({"id_hogar": id_hogar, "total": len(resultado),
                        "inventario": resultado}), 200

    except Exception as e:
        return jsonify({"error": "Error interno", "detalle": detalle(e)}), 500
# ====================== INVENTARIO: EDITAR / ELIMINAR / ESTADÍSTICAS ======================
ESTADOS_VALIDOS = ("disponible", "consumido", "desechado")


def buscar_item(id_hogar, id_item):
    """Devuelve el ítem solo si pertenece a ese hogar (evita tocar ítems de otros hogares)."""
    return Inventario.query.filter_by(id_item=id_item, id_hogar=id_hogar).first()


@app.route("/api/hogares/<int:id_hogar>/inventario/<int:id_item>", methods=["PATCH"])
@jwt_required()
def actualizar_item(id_hogar, id_item):
    try:
        if not db.session.get(Hogar, id_hogar):
            return jsonify({"error": "Hogar no encontrado"}), 404
        if not es_miembro(id_hogar, usuario_actual()):
            return jsonify({"error": "No perteneces a este hogar"}), 403

        item = buscar_item(id_hogar, id_item)
        if not item:
            return jsonify({"error": "Alimento no encontrado en este hogar"}), 404

        data = request.get_json(silent=True) or {}
        if not data:
            return jsonify({"error": "Envía al menos un campo a modificar"}), 400

        if "estado" in data:
            if data["estado"] not in ESTADOS_VALIDOS:
                return jsonify({"error": f"estado debe ser uno de {list(ESTADOS_VALIDOS)}"}), 400
            # Un ítem que vuelve a 'disponible' ocupa lugar: respeta el límite del plan
            if data["estado"] == "disponible" and item.estado != "disponible":
                plan = plan_del_hogar(id_hogar)
                if (plan.max_items_inventario is not None
                        and items_en(id_hogar) >= plan.max_items_inventario):
                    return jsonify({
                        "error": f"Alcanzaste el límite de {plan.max_items_inventario} "
                                 f"alimentos del plan {plan.nombre}",
                        "codigo": "limite_plan"
                    }), 403
            item.estado = data["estado"]

        if "cantidad" in data:
            try:
                cantidad = float(data["cantidad"])
            except (TypeError, ValueError):
                return jsonify({"error": "cantidad debe ser un número"}), 400
            if cantidad <= 0:
                return jsonify({"error": "cantidad debe ser mayor que 0"}), 400
            item.cantidad = cantidad

        if "fecha_caducidad" in data:
            try:
                item.fecha_caducidad = datetime.strptime(
                    data["fecha_caducidad"], "%Y-%m-%d").date()
            except (TypeError, ValueError):
                return jsonify({"error": "Formato de fecha inválido, usa YYYY-MM-DD"}), 400

        db.session.commit()
        return jsonify({
            "mensaje": "Alimento actualizado",
            "id_item": item.id_item,
            "estado": item.estado,
            "cantidad": float(item.cantidad),
            "fecha_caducidad": item.fecha_caducidad.isoformat()
        }), 200

    except Exception as e:
        db.session.rollback()
        return jsonify({"error": "Error interno", "detalle": detalle(e)}), 500


@app.route("/api/hogares/<int:id_hogar>/inventario/<int:id_item>", methods=["DELETE"])
@jwt_required()
def eliminar_item(id_hogar, id_item):
    try:
        if not db.session.get(Hogar, id_hogar):
            return jsonify({"error": "Hogar no encontrado"}), 404
        if not es_miembro(id_hogar, usuario_actual()):
            return jsonify({"error": "No perteneces a este hogar"}), 403

        item = buscar_item(id_hogar, id_item)
        if not item:
            return jsonify({"error": "Alimento no encontrado en este hogar"}), 404

        db.session.delete(item)
        db.session.commit()
        return jsonify({"mensaje": "Alimento eliminado", "id_item": id_item}), 200

    except Exception as e:
        db.session.rollback()
        return jsonify({"error": "Error interno", "detalle": detalle(e)}), 500


@app.route("/api/hogares/<int:id_hogar>/estadisticas", methods=["GET"])
@jwt_required()
def estadisticas(id_hogar):
    try:
        if not db.session.get(Hogar, id_hogar):
            return jsonify({"error": "Hogar no encontrado"}), 404
        if not es_miembro(id_hogar, usuario_actual()):
            return jsonify({"error": "No perteneces a este hogar"}), 403

        items = Inventario.query.filter_by(id_hogar=id_hogar).all()
        hoy = date.today()

        conteo = {"disponible": 0, "consumido": 0, "desechado": 0}
        for i in items:
            conteo[i.estado] += 1

        por_vencer = sum(1 for i in items if i.estado == "disponible"
                         and 0 <= (i.fecha_caducidad - hoy).days <= 3)
        caducados = sum(1 for i in items if i.estado == "disponible"
                        and i.fecha_caducidad < hoy)

        finalizados = conteo["consumido"] + conteo["desechado"]
        pct_aprovechado = (round(conteo["consumido"] * 100 / finalizados, 1)
                           if finalizados else None)

        return jsonify({
            "id_hogar": id_hogar,
            "disponibles": conteo["disponible"],
            "consumidos": conteo["consumido"],
            "desechados": conteo["desechado"],
            "por_vencer_3_dias": por_vencer,
            "caducados_sin_desechar": caducados,
            "porcentaje_aprovechado": pct_aprovechado   # null si aún no hay historial
        }), 200

    except Exception as e:
        return jsonify({"error": "Error interno", "detalle": detalle(e)}), 500
# ====================== CATÁLOGO (solo lectura) ======================
def _categoria_json(c):
    return {"id_categoria": c.id_categoria, "nombre": c.nombre}


def _unidad_json(u):
    return {"id_unidad": u.id_unidad, "nombre": u.nombre, "abreviatura": u.abreviatura}


def _producto_json(p):
    return {
        "id_producto": p.id_producto,
        "nombre": p.nombre,
        "id_categoria": p.id_categoria,
        "categoria": p.categoria.nombre,
        "dias_vida_util": p.dias_vida_util,
        "personalizado": p.id_hogar is not None,
        "id_hogar": p.id_hogar
    }

@app.route("/api/categorias", methods=["GET"])
@jwt_required()
def listar_categorias():
    try:
        cats = Categoria.query.order_by(Categoria.nombre.asc()).all()
        return jsonify({"total": len(cats),
                        "categorias": [_categoria_json(c) for c in cats]}), 200
    except Exception as e:
        return jsonify({"error": "Error interno", "detalle": detalle(e)}), 500


@app.route("/api/unidades", methods=["GET"])
@jwt_required()
def listar_unidades():
    try:
        unis = UnidadMedida.query.order_by(UnidadMedida.nombre.asc()).all()
        return jsonify({"total": len(unis),
                        "unidades": [_unidad_json(u) for u in unis]}), 200
    except Exception as e:
        return jsonify({"error": "Error interno", "detalle": detalle(e)}), 500

def hogar_de_parametro():
    """Lee ?hogar=ID. Devuelve (id_hogar, None), (None, None) si no viene, o (None, error)."""
    if "hogar" not in request.args:
        return None, None
    id_hogar = request.args.get("hogar", type=int)
    if id_hogar is None:
        return None, (jsonify({"error": "hogar debe ser un número"}), 400)
    if not db.session.get(Hogar, id_hogar):
        return None, (jsonify({"error": "Hogar no encontrado"}), 404)
    if not es_miembro(id_hogar, usuario_actual()):
        return None, (jsonify({"error": "No perteneces a este hogar"}), 403)
    return id_hogar, None


def productos_visibles(id_hogar=None):
    """Globales + personalizados del hogar indicado (o de todos los hogares del usuario)."""
    if id_hogar is not None:
        ids = [id_hogar]
    else:
        ids = [m.id_hogar for m in
               HogarMiembro.query.filter_by(id_usuario=usuario_actual()).all()]
    return Producto.query.filter(
        or_(Producto.id_hogar.is_(None), Producto.id_hogar.in_(ids)))
@app.route("/api/productos", methods=["GET"])
@jwt_required()
def listar_productos():
    try:
        id_hogar, error = hogar_de_parametro()
        if error:
            return error
        query = productos_visibles(id_hogar)

        if "categoria" in request.args:
            id_categoria = request.args.get("categoria", type=int)
            if id_categoria is None:
                return jsonify({"error": "categoria debe ser un número"}), 400
            if not db.session.get(Categoria, id_categoria):
                return jsonify({"error": "Categoría no encontrada"}), 404
            query = query.filter(Producto.id_categoria == id_categoria)

        texto = (request.args.get("q") or "").strip()
        if texto:
            query = query.filter(Producto.nombre.like(f"%{texto}%"))

        productos = query.order_by(Producto.nombre.asc()).all()
        return jsonify({"total": len(productos),
                        "productos": [_producto_json(p) for p in productos]}), 200
    except Exception as e:
        return jsonify({"error": "Error interno", "detalle": detalle(e)}), 500


@app.route("/api/catalogo", methods=["GET"])
@jwt_required()
def catalogo_completo():
    try:
        id_hogar, error = hogar_de_parametro()
        if error:
            return error
        cats = Categoria.query.order_by(Categoria.nombre.asc()).all()
        unis = UnidadMedida.query.order_by(UnidadMedida.nombre.asc()).all()
        prods = productos_visibles(id_hogar).order_by(Producto.nombre.asc()).all()
        return jsonify({
            "categorias": [_categoria_json(c) for c in cats],
            "unidades": [_unidad_json(u) for u in unis],
            "productos": [_producto_json(p) for p in prods]
        }), 200
    except Exception as e:
        return jsonify({"error": "Error interno", "detalle": detalle(e)}), 500
# ====================== PRODUCTOS PERSONALIZADOS ======================
LIMITE_PRODUCTOS_FREE = 10


@app.route("/api/hogares/<int:id_hogar>/productos", methods=["POST"])
@jwt_required()
def crear_producto_personalizado(id_hogar):
    try:
        if not db.session.get(Hogar, id_hogar):
            return jsonify({"error": "Hogar no encontrado"}), 404
        if not es_miembro(id_hogar, usuario_actual()):
            return jsonify({"error": "No perteneces a este hogar"}), 403

        data = request.get_json(silent=True) or {}
        nombre = (data.get("nombre") or "").strip()
        if not nombre or len(nombre) > 120:
            return jsonify({"error": "nombre es obligatorio (máximo 120 caracteres)"}), 400

        try:
            id_categoria = int(data.get("id_categoria"))
            dias = int(data.get("dias_vida_util"))
        except (TypeError, ValueError):
            return jsonify({"error": "id_categoria y dias_vida_util deben ser números"}), 400
        if not 1 <= dias <= 3650:
            return jsonify({"error": "dias_vida_util debe estar entre 1 y 3650"}), 400
        if not db.session.get(Categoria, id_categoria):
            return jsonify({"error": "Categoría no encontrada"}), 404

        # No repetir un nombre del catálogo global ni de este hogar
        # (la comparación ignora mayúsculas y acentos por la collation)
        existe = Producto.query.filter(
            Producto.nombre == nombre,
            or_(Producto.id_hogar.is_(None), Producto.id_hogar == id_hogar)).first()
        if existe:
            return jsonify({"error": f"Ya existe un producto llamado '{existe.nombre}'",
                            "id_producto": existe.id_producto}), 409

        plan = plan_del_hogar(id_hogar)
        if plan.nombre == "Free":
            total = Producto.query.filter_by(id_hogar=id_hogar).count()
            if total >= LIMITE_PRODUCTOS_FREE:
                return jsonify({
                    "error": f"El plan Free permite {LIMITE_PRODUCTOS_FREE} productos "
                             f"personalizados. Pasa a Premium para crear más",
                    "codigo": "limite_plan"
                }), 403

        producto = Producto(nombre=nombre, id_categoria=id_categoria,
                            dias_vida_util=dias, id_hogar=id_hogar)
        db.session.add(producto)
        db.session.commit()
        return jsonify({"mensaje": "Producto creado",
                        "producto": _producto_json(producto)}), 201

    except Exception as e:
        db.session.rollback()
        return jsonify({"error": "Error interno", "detalle": detalle(e)}), 500


@app.route("/api/hogares/<int:id_hogar>/productos/<int:id_producto>", methods=["DELETE"])
@jwt_required()
def eliminar_producto_personalizado(id_hogar, id_producto):
    try:
        if not db.session.get(Hogar, id_hogar):
            return jsonify({"error": "Hogar no encontrado"}), 404
        if not es_miembro(id_hogar, usuario_actual()):
            return jsonify({"error": "No perteneces a este hogar"}), 403

        # Filtra por hogar: los productos globales no se pueden borrar desde aquí
        producto = Producto.query.filter_by(
            id_producto=id_producto, id_hogar=id_hogar).first()
        if not producto:
            return jsonify({"error": "Producto personalizado no encontrado"}), 404

        if Inventario.query.filter_by(id_producto=id_producto).first():
            return jsonify({
                "error": "El producto está en uso en el inventario (o su historial) "
                         "y no se puede eliminar"
            }), 409

        db.session.delete(producto)
        db.session.commit()
        return jsonify({"mensaje": "Producto eliminado", "id_producto": id_producto}), 200

    except Exception as e:
        db.session.rollback()
        return jsonify({"error": "Error interno", "detalle": detalle(e)}), 500

if __name__ == "__main__":
    app.run(host='0.0.0.0', port=5000, debug=True)
