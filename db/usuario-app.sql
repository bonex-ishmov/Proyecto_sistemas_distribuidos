-- =====================================================================
-- db/usuario-app.sql  --  Tienda Generica
-- Permisos del usuario 'tienda', el que usan los microservicios.
-- Ejecutar DESPUES de db/schema.sql (los esquemas deben existir).
--
-- El usuario lo crea la imagen de MySQL a partir de MYSQL_USER /
-- MYSQL_PASSWORD (docker-compose.yml + .env), asi la clave no queda en git.
--
-- Solo SELECT/INSERT/UPDATE/DELETE: los servicios corren con
-- ddl-auto=validate y nunca crean ni alteran tablas. La carga CSV de
-- ms-catalogo vacia productos con deleteAllInBatch(), que es un DELETE
-- (no TRUNCATE), asi que no necesita mas permisos.
-- =====================================================================

GRANT SELECT, INSERT, UPDATE, DELETE ON db_identidad.* TO 'tienda'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON db_clientes.*  TO 'tienda'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON db_catalogo.*  TO 'tienda'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON db_ventas.*    TO 'tienda'@'%';
