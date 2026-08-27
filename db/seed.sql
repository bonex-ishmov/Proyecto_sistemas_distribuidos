-- =====================================================================
-- db/seed.sql  --  Tienda Generica
-- Datos semilla minimos. Ejecutar DESPUES de db/schema.sql.
-- Idempotente: ON DUPLICATE KEY UPDATE deja no-op si la fila ya existe.
--
-- NOTA: el usuario inicial (admininicial / admin123456) NO se siembra aqui.
-- Lo crea DataInitializer de ms-identidad al arrancar, hasheando la clave
-- con BCrypt (idempotente: no hace nada si ya existe).
-- =====================================================================

-- --- Los 5 proveedores que referencia ms-catalogo/productos.csv ---
-- (movidos desde el antiguo ms-catalogo/setup-db.sql)
INSERT INTO db_catalogo.proveedores
    (nitproveedor, nombre_proveedor, direccion_proveedor, telefono_proveedor, ciudad_proveedor)
VALUES
    (1, 'Frutas del Campo S.A.S.',    'Carrera 68 # 45-12',  '6014567890', 'Bogotá'),
    (2, 'Verduras Frescas Ltda.',     'Calle 13 # 30-25',    '6012223344', 'Bogotá'),
    (3, 'Cosecha Andina S.A.',        'Avenida 30 # 8-20',   '6045556677', 'Medellín'),
    (4, 'Tropical Import S.A.S.',     'Carrera 100 # 5-40',  '6028889900', 'Cali'),
    (5, 'Lácteos y Carnes del Norte', 'Diagonal 45 # 12-08', '6051112233', 'Barranquilla')
ON DUPLICATE KEY UPDATE nombre_proveedor = VALUES(nombre_proveedor);
