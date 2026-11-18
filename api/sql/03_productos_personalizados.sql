USE despensa_inteligente;

-- id_hogar NULL = producto global; con valor = producto personalizado de ese hogar
ALTER TABLE productos
  ADD COLUMN id_hogar INT NULL AFTER dias_vida_util;

-- El nombre era único en todo el sistema; ahora lo es por hogar
ALTER TABLE productos DROP INDEX nombre;
ALTER TABLE productos ADD UNIQUE KEY uq_producto_hogar (nombre, id_hogar);

ALTER TABLE productos
  ADD CONSTRAINT fk_prod_hogar FOREIGN KEY (id_hogar)
    REFERENCES hogares(id_hogar) ON DELETE CASCADE;
