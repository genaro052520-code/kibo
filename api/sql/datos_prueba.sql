-- =====================================================
-- Datos de prueba: Despensa Inteligente
-- Fechas relativas a hoy (CURDATE)
-- =====================================================
USE despensa_inteligente;

-- ---------- Productos adicionales al catálogo ----------
INSERT IGNORE INTO productos (nombre, id_categoria, dias_vida_util) VALUES
 ('Queso fresco', 1, 10),
 ('Crema', 1, 12),
 ('Carne molida', 2, 2),
 ('Jamón', 2, 7),
 ('Plátano', 3, 6),
 ('Fresas', 3, 4),
 ('Jitomate', 4, 7),
 ('Zanahoria', 4, 21),
 ('Espinaca', 4, 4),
 ('Sopa recalentable', 5, 3);

-- ---------- Usuarios ----------
-- Contraseña de todos: 123456 (hash de prueba, NO válido para login real).
-- Cuando hagamos el login, los crearemos desde la API para generar hashes correctos.
INSERT INTO usuarios (nombre, email, password_hash, id_plan) VALUES
 ('Carlos Ramírez', 'carlos@correo.com',  'hash_de_prueba', 1),
 ('Lucía Ramírez',  'lucia@correo.com',   'hash_de_prueba', 1),
 ('María Torres',   'maria@correo.com',   'hash_de_prueba', 2),
 ('Pedro Torres',   'pedro@correo.com',   'hash_de_prueba', 2),
 ('Sofía Mendoza',  'sofia@correo.com',   'hash_de_prueba', 1),
 ('Diego Mendoza',  'diego@correo.com',   'hash_de_prueba', 1);

-- ---------- Hogares ----------
INSERT INTO hogares (nombre) VALUES
 ('Casa Ramírez'),
 ('Familia Torres'),
 ('Depa Mendoza');

-- ---------- Miembros (usamos subconsultas para no depender de los IDs) ----------
INSERT INTO hogar_miembros (id_hogar, id_usuario, rol)
SELECT h.id_hogar, u.id_usuario, x.rol
FROM (
  SELECT 'Casa Ramírez' AS hogar, 'carlos@correo.com' AS email, 'admin'   AS rol UNION ALL
  SELECT 'Casa Ramírez',          'lucia@correo.com',            'miembro'        UNION ALL
  SELECT 'Familia Torres',        'maria@correo.com',            'admin'          UNION ALL
  SELECT 'Familia Torres',        'pedro@correo.com',            'miembro'        UNION ALL
  SELECT 'Depa Mendoza',          'sofia@correo.com',            'admin'          UNION ALL
  SELECT 'Depa Mendoza',          'diego@correo.com',            'miembro'
) x
JOIN hogares  h ON h.nombre = x.hogar
JOIN usuarios u ON u.email  = x.email;

-- ---------- Inventario ----------
-- Formato: hogar, producto, unidad, quién lo agregó, cantidad, días desde que se compró, días hasta que caduca
-- (días_hasta_caducar negativo = ya caducado)
INSERT INTO inventario
  (id_hogar, id_producto, id_unidad, agregado_por, cantidad, fecha_ingreso, fecha_caducidad)
SELECT h.id_hogar, p.id_producto, un.id_unidad, u.id_usuario,
       x.cantidad,
       DATE_SUB(CURDATE(), INTERVAL x.hace DAY),
       DATE_ADD(CURDATE(), INTERVAL x.vence DAY)
FROM (
  -- ===== Casa Ramírez =====
  SELECT 'Casa Ramírez' AS hogar, 'Leche entera' AS prod, 'L'   AS uni, 'carlos@correo.com' AS email, 2   AS cantidad, 6  AS hace, 1   AS vence UNION ALL
  SELECT 'Casa Ramírez', 'Pechuga de pollo', 'kg',  'lucia@correo.com',   1.5,  2,  1  UNION ALL
  SELECT 'Casa Ramírez', 'Carne molida',     'kg',  'carlos@correo.com',  0.5,  3, -1  UNION ALL
  SELECT 'Casa Ramírez', 'Yogur natural',    'pza', 'lucia@correo.com',   4,    4,  10 UNION ALL
  SELECT 'Casa Ramírez', 'Manzana',          'pza', 'carlos@correo.com',  6,    5,  15 UNION ALL
  SELECT 'Casa Ramírez', 'Lechuga',          'pza', 'lucia@correo.com',   1,    4,  1  UNION ALL
  SELECT 'Casa Ramírez', 'Jitomate',         'kg',  'carlos@correo.com',  1,    2,  5  UNION ALL
  SELECT 'Casa Ramírez', 'Arroz cocinado',   'g',   'lucia@correo.com',   500,  2,  1  UNION ALL
  -- ===== Familia Torres =====
  SELECT 'Familia Torres', 'Queso fresco',   'g',   'maria@correo.com',   400,  3,  7  UNION ALL
  SELECT 'Familia Torres', 'Crema',          'L',   'pedro@correo.com',   0.5,  10, 2  UNION ALL
  SELECT 'Familia Torres', 'Jamón',          'g',   'maria@correo.com',   250,  6,  1  UNION ALL
  SELECT 'Familia Torres', 'Plátano',        'pza', 'pedro@correo.com',   6,    5,  1  UNION ALL
  SELECT 'Familia Torres', 'Fresas',         'g',   'maria@correo.com',   500,  3,  1  UNION ALL
  SELECT 'Familia Torres', 'Zanahoria',      'kg',  'pedro@correo.com',   1,    2,  19 UNION ALL
  SELECT 'Familia Torres', 'Leche entera',   'L',   'maria@correo.com',   3,    1,  6  UNION ALL
  SELECT 'Familia Torres', 'Sopa recalentable','pza','pedro@correo.com',  2,    4, -1  UNION ALL
  -- ===== Depa Mendoza =====
  SELECT 'Depa Mendoza', 'Espinaca',         'g',   'sofia@correo.com',   300,  3,  1  UNION ALL
  SELECT 'Depa Mendoza', 'Yogur natural',    'pza', 'diego@correo.com',   6,    1,  13 UNION ALL
  SELECT 'Depa Mendoza', 'Pechuga de pollo', 'kg',  'sofia@correo.com',   1,    0,  3  UNION ALL
  SELECT 'Depa Mendoza', 'Manzana',          'pza', 'diego@correo.com',   4,    2,  18
) x
JOIN hogares        h  ON h.nombre = x.hogar
JOIN productos      p  ON p.nombre = x.prod
JOIN unidades_medida un ON un.abreviatura = x.uni
JOIN usuarios       u  ON u.email  = x.email;
