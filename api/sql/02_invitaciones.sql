USE despensa_inteligente;

-- Códigos de invitación para unirse a un hogar (caducan a los 7 días)
CREATE TABLE IF NOT EXISTS invitaciones (
  id_invitacion INT AUTO_INCREMENT PRIMARY KEY,
  id_hogar INT NOT NULL,
  codigo VARCHAR(12) NOT NULL UNIQUE,
  creado_por INT NULL,
  fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_expiracion DATETIME NOT NULL,
  CONSTRAINT fk_inv_cod_hogar FOREIGN KEY (id_hogar)
    REFERENCES hogares(id_hogar) ON DELETE CASCADE,
  CONSTRAINT fk_inv_cod_user FOREIGN KEY (creado_por)
    REFERENCES usuarios(id_usuario) ON DELETE SET NULL
) ENGINE=InnoDB;
