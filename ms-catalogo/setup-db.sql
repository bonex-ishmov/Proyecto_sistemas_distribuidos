-- ============================================================
-- ms-catalogo : creacion del esquema y datos minimos
-- ============================================================

CREATE DATABASE IF NOT EXISTS db_catalogo
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

-- Las tablas las crea Hibernate al arrancar (ddl-auto=update).
-- Ejecuta el servicio UNA vez y luego vuelve aqui.

-- Los 5 proveedores que referencia el CSV de muestra del documento.
-- Sin ellos, la carga de productos falla con "el proveedor no existe".
-- Se pueden crear tambien por POST /api/proveedores.
INSERT INTO db_catalogo.proveedores
    (nitproveedor, nombre_proveedor, direccion_proveedor, telefono_proveedor, ciudad_proveedor)
VALUES
    (1, 'Frutas del Campo S.A.S.',    'Carrera 68 # 45-12',  '6014567890', 'Bogotá'),
    (2, 'Verduras Frescas Ltda.',     'Calle 13 # 30-25',    '6012223344', 'Bogotá'),
    (3, 'Cosecha Andina S.A.',        'Avenida 30 # 8-20',   '6045556677', 'Medellín'),
    (4, 'Tropical Import S.A.S.',     'Carrera 100 # 5-40',  '6028889900', 'Cali'),
    (5, 'Lácteos y Carnes del Norte', 'Diagonal 45 # 12-08', '6051112233', 'Barranquilla')
ON DUPLICATE KEY UPDATE nombre_proveedor = VALUES(nombre_proveedor);

-- Verificaciones utiles
-- SELECT * FROM db_catalogo.proveedores;
-- SELECT * FROM db_catalogo.productos;
-- SHOW CREATE TABLE db_catalogo.productos;   -- debe mostrar fk_producto_proveedor
