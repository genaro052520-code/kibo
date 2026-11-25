USE despensa_inteligente;

-- Otros nombres con los que la gente llama a un producto ("tomate" = Jitomate)
CREATE TABLE IF NOT EXISTS alias_productos (
  id_alias INT AUTO_INCREMENT PRIMARY KEY,
  id_producto INT NOT NULL,
  alias VARCHAR(120) NOT NULL,
  UNIQUE KEY uq_alias_producto (id_producto, alias),
  INDEX idx_alias (alias),
  CONSTRAINT fk_alias_producto FOREIGN KEY (id_producto)
    REFERENCES productos(id_producto) ON DELETE CASCADE
) ENGINE=InnoDB;
