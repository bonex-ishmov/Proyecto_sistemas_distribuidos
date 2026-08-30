-- =====================================================================
-- db/schema.sql  --  Tienda Generica
-- Servidor: MySQL 9.4 (Railway, compartido por el equipo)
-- Crea los 4 esquemas y todas sus tablas. Idempotente (IF NOT EXISTS):
-- se puede volver a ejecutar sin efecto si ya existen.
-- Derivado de las entidades JPA (@Entity) de los cuatro modulos.
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


-- =========================  db_ventas  ==============================
CREATE DATABASE IF NOT EXISTS db_ventas
    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS db_ventas.ventas (
    -- Unica PK autogenerada del proyecto: el PDF pide un consecutivo de venta.
    -- Las demas tablas usan clave natural (cedula, NIT, codigo de producto).
    codigo_venta    BIGINT         NOT NULL AUTO_INCREMENT,
    cedula_cliente  BIGINT         NOT NULL,
    cedula_usuario  BIGINT         NOT NULL,
    -- Base gravable de la venta: suma de detalle_ventas.valor_total (cantidad x
    -- valor_unitario de cada linea), SIN IVA. El IVA va aparte, en valor_iva.
    total_venta     DECIMAL(15,2)  NOT NULL,
    valor_iva       DECIMAL(15,2)  NOT NULL,
    total_con_iva   DECIMAL(15,2)  NOT NULL,
    fecha_venta     DATETIME(6)    NOT NULL,
    PRIMARY KEY (codigo_venta),
    KEY idx_ventas_cedula_cliente (cedula_cliente)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS db_ventas.detalle_ventas (
    id_detalle      BIGINT         NOT NULL AUTO_INCREMENT,
    codigo_venta    BIGINT         NOT NULL,
    codigo_producto BIGINT         NOT NULL,
    -- Snapshot deliberado: POST /api/productos/carga borra la tabla productos
    -- completa, asi que el nombre se congela al momento de la venta para que la
    -- venta siga siendo legible aunque el producto desaparezca del catalogo.
    nombre_producto VARCHAR(50)    NOT NULL,
    cantidad        INT            NOT NULL,
    valor_unitario  DECIMAL(15,2)  NOT NULL,
    iva_porcentaje  DECIMAL(5,2)   NOT NULL,
    valor_iva       DECIMAL(15,2)  NOT NULL,
    -- Base SIN IVA de la linea: cantidad x valor_unitario. El IVA de la linea va
    -- aparte, en la columna valor_iva de arriba. El nombre "valor_total" viene
    -- del PDF y es ambiguo: NO incluye IVA.
    valor_total     DECIMAL(15,2)  NOT NULL,
    PRIMARY KEY (id_detalle),
    KEY idx_detalle_ventas_codigo_venta (codigo_venta),
    CONSTRAINT fk_detalle_venta
        FOREIGN KEY (codigo_venta)
        REFERENCES db_ventas.ventas (codigo_venta)
) ENGINE=InnoDB;
