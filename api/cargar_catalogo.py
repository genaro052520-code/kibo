"""Carga api/sql/catalogo_productos.csv a la base.
Se puede ejecutar varias veces: no duplica productos ni alias."""
import csv
from pathlib import Path

from app import app
from models import db, Alias, Categoria, Producto

RUTA = Path(__file__).parent / "sql" / "catalogo_productos.csv"


def main():
    nuevos = sin_cambios = actualizados = 0
    categorias_nuevas = alias_nuevos = 0
    avisos = []

    with app.app_context():
        try:
            with open(RUTA, encoding="utf-8-sig", newline="") as f:
                filas = list(csv.DictReader(f))

            for n, fila in enumerate(filas, start=2):   # la línea 1 es el encabezado
                nombre = (fila.get("nombre") or "").strip()
                nombre_cat = (fila.get("categoria") or "").strip()
                try:
                    dias = int(fila.get("dias_vida_util"))
                except (TypeError, ValueError):
                    dias = 0
                if not nombre or not nombre_cat or dias < 1:
                    avisos.append(f"Línea {n}: datos inválidos para '{nombre}', se omitió")
                    continue

                # Categoría (la comparación ignora mayúsculas y acentos)
                categoria = Categoria.query.filter(Categoria.nombre == nombre_cat).first()
                if not categoria:
                    categoria = Categoria(nombre=nombre_cat)
                    db.session.add(categoria)
                    db.session.flush()
                    categorias_nuevas += 1

                # Producto global (id_hogar vacío)
                producto = Producto.query.filter(
                    Producto.nombre == nombre, Producto.id_hogar.is_(None)).first()
                if producto is None:
                    producto = Producto(nombre=nombre, id_categoria=categoria.id_categoria,
                                        dias_vida_util=dias)
                    db.session.add(producto)
                    db.session.flush()
                    nuevos += 1
                elif (producto.id_categoria != categoria.id_categoria
                      or producto.dias_vida_util != dias):
                    producto.id_categoria = categoria.id_categoria
                    producto.dias_vida_util = dias
                    actualizados += 1
                else:
                    sin_cambios += 1

                # Alias
                for alias in (fila.get("alias") or "").split("|"):
                    alias = alias.strip()
                    if not alias or len(alias) > 120 or alias.lower() == nombre.lower():
                        continue
                    ya = Alias.query.filter(Alias.id_producto == producto.id_producto,
                                            Alias.alias == alias).first()
                    if ya:
                        continue
                    choque = (db.session.query(Producto.nombre)
                              .join(Alias, Alias.id_producto == Producto.id_producto)
                              .filter(Alias.alias == alias, Producto.id_hogar.is_(None),
                                      Producto.id_producto != producto.id_producto)
                              .first())
                    if choque:
                        avisos.append(f"Alias '{alias}' de '{nombre}' ya lo usa "
                                      f"'{choque[0]}' (se agregó igual)")
                    db.session.add(Alias(id_producto=producto.id_producto, alias=alias))
                    alias_nuevos += 1

            db.session.commit()
        except Exception:
            db.session.rollback()
            raise

    print(f"Productos nuevos:      {nuevos}")
    print(f"Productos sin cambios: {sin_cambios}")
    print(f"Productos actualizados:{actualizados}")
    print(f"Categorías nuevas:     {categorias_nuevas}")
    print(f"Alias nuevos:          {alias_nuevos}")
    for a in avisos:
        print("AVISO:", a)


if __name__ == "__main__":
    main()
