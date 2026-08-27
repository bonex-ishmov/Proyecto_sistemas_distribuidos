-- =====================================================================
-- db/schema.sql  --  Tienda Generica
-- Servidor: MySQL 9.4 (Railway, compartido por el equipo)
-- Crea los 3 esquemas y todas sus tablas. Idempotente (IF NOT EXISTS):
-- se puede volver a ejecutar sin efecto si ya existen.
-- Derivado de las entidades JPA (@Entity) de los tres modulos.
-- Los servicios corren con spring.jpa.hibernate.ddl-auto=validate:
-- este script es la UNICA fuente del esquema.
--   Orden de ejecucion:  1) db/schema.sql   2) db/seed.sql
-- =====================================================================


-- =========================  db_identidad  ============================
CREATE DATABASE IF NOT EXISTS db_identidad
    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS db_identidad.usuarios (
    cedula_usuario  BIGINT        NOT NULL,
    nombre_usuario  VARCHAR(100)  NOT NULL,
    email_usuario   VARCHAR(100)  NOT NULL,
    usuario         VARCHAR(50)   NOT NULL,
    `password`      VARCHAR(255)  NOT NULL,
    activo          BIT(1)        NOT NULL,
    PRIMARY KEY (cedula_usuario),
    CONSTRAINT uk_usuarios_email_usuario UNIQUE (email_usuario),
    CONSTRAINT uk_usuarios_usuario       UNIQUE (usuario)
) ENGINE=InnoDB;


-- =========================  db_clientes  =============================
CREATE DATABASE IF NOT EXISTS db_clientes
    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS db_clientes.clientes (
    cedula_cliente     BIGINT        NOT NULL,
    nombre_cliente     VARCHAR(100)  NOT NULL,
    direccion_cliente  VARCHAR(150)  NOT NULL,
    telefono_cliente   VARCHAR(20)   NOT NULL,
    email_cliente      VARCHAR(100)  NOT NULL,
    PRIMARY KEY (cedula_cliente)
) ENGINE=InnoDB;


-- =========================  db_catalogo  =============================
CREATE DATABASE IF NOT EXISTS db_catalogo
    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS db_catalogo.proveedores (
    nitproveedor         BIGINT        NOT NULL,
    nombre_proveedor     VARCHAR(100)  NOT NULL,
    direccion_proveedor  VARCHAR(150)  NOT NULL,
    telefono_proveedor   VARCHAR(20)   NOT NULL,
    ciudad_proveedor     VARCHAR(60)   NOT NULL,
    PRIMARY KEY (nitproveedor)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS db_catalogo.productos (
    codigo_producto  BIGINT         NOT NULL,
    nombre_producto  VARCHAR(50)    NOT NULL,
    nitproveedor     BIGINT         NOT NULL,
    precio_compra    DECIMAL(15,2)  NOT NULL,
    ivacompra        DECIMAL(5,2)   NOT NULL,
    precio_venta     DECIMAL(15,2)  NOT NULL,
    PRIMARY KEY (codigo_producto),
    KEY idx_producto_nitproveedor (nitproveedor),
    CONSTRAINT fk_producto_proveedor
        FOREIGN KEY (nitproveedor)
        REFERENCES db_catalogo.proveedores (nitproveedor)
) ENGINE=InnoDB;
