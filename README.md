# Tienda Genérica — monorepo de microservicios

Cinco microservicios Spring Boot independientes (Java 25, Spring Boot 4.1.1) para
la gestión de una tienda genérica. Arquitectura y decisiones de diseño en
`CLAUDE.md`; estado del proyecto en `docs/diagnostico.md`.

| Módulo        | Puerto | Esquema      |
|---------------|--------|--------------|
| ms-identidad  | 8081   | db_identidad |
| ms-clientes   | 8082   | db_clientes  |
| ms-catalogo   | 8083   | db_catalogo  |
| ms-ventas     | 8084   | db_ventas    |
| ms-reportes   | 8085   | — (sin esquema ni BD propia) |

> `ms-reportes` es un agregador de solo lectura (módulo 7: consultas y reportes):
> no persiste nada, así que su `application-local.properties` no lleva
> `spring.datasource.*`. En su lugar necesita `app.jwt.secret` (igual que los
> demás módulos) y `ms.identidad.usuario-sistema` / `ms.identidad.password-sistema`:
> las credenciales del **mismo usuario de sistema que usa `ms-ventas`**
> (`ventas-sistema`) para autenticarse contra ms-identidad, ms-clientes y
> ms-ventas al armar cada reporte.

## Base de datos

MySQL **9.4 remoto en Railway**, **compartido por todo el equipo**.
Host: `altaria.proxy.rlwy.net:40105`.

Los servicios arrancan con `spring.jpa.hibernate.ddl-auto=validate`: **no crean ni
modifican tablas**. El esquema se administra con scripts versionados:

| Script          | Qué hace                                                                            | Cuándo se corre                                     |
|-----------------|-----------------------------------------------------------------------------------|----------------------------------------------------|
| `db/schema.sql` | Crea los 4 esquemas (`ms-reportes` no tiene) y todas sus tablas, índices y FKs. Idempotente. | Una vez al montar la base; y al cambiar una entidad. |
| `db/seed.sql`   | Inserta los 5 proveedores que referencia `ms-catalogo/productos.csv`. Idempotente. | Una vez, después de `schema.sql`.                   |

> El usuario inicial `admininicial` / `admin123456` lo crea `DataInitializer` de
> `ms-identidad` al arrancar (hashea la clave con BCrypt; no hace nada si ya existe).

### ⚠️ La base es COMPARTIDA

- **`POST /api/productos/carga` borra la tabla `productos` completa** y la recarga
  desde el CSV. **Nadie debe ejecutarlo mientras el otro integrante está
  trabajando.** Avisar por el canal del equipo antes de usarlo.
- No cambiar `ddl-auto` a `update` "para probar rápido": alteraría el esquema
  compartido sin que el otro se entere. Si `validate` falla es porque una entidad
  y `db/schema.sql` divergieron: hay que actualizar el script y coordinar el
  cambio de tabla con el equipo.

## Configurar un entorno nuevo

### 1. Requisitos
- JDK 25
- Un cliente MySQL (Workbench, DBeaver o el CLI `mysql`)
- Credenciales de la base: las pasa el equipo por canal privado, **no van al repo**

### 2. Crear esquema y datos (una sola persona, una vez)
```
mysql -h altaria.proxy.rlwy.net -P 40105 -u <user> -p < db/schema.sql
mysql -h altaria.proxy.rlwy.net -P 40105 -u <user> -p < db/seed.sql
```

### 3. Configurar cada módulo
Cada módulo lee sus credenciales de un archivo **no versionado**
`src/main/resources/application-local.properties`. En cada módulo:

```
cp src/main/resources/application-local.properties.example \
   src/main/resources/application-local.properties
```

y editá el archivo copiado con los valores reales. Aplica a `ms-identidad`,
`ms-clientes`, `ms-catalogo` y `ms-ventas`; `ms-reportes` no tiene base de datos
propia y usa credenciales distintas (ver la nota más arriba):

| Propiedad                    | Valor                                                                                        |
|------------------------------|--------------------------------------------------------------------------------------------|
| `spring.datasource.url`      | `jdbc:mysql://altaria.proxy.rlwy.net:40105/<esquema>?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true` — `<esquema>` según el módulo; **`useSSL=true` obligatorio** |
| `spring.datasource.username` | `root`                                                                                    |
| `spring.datasource.password` | la comparte el equipo                                                                     |
| `app.jwt.secret`             | cadena de 64 caracteres, **idéntica en los cinco módulos** (incluido `ms-reportes`)         |

`application.properties` lo importa con
`spring.config.import=optional:classpath:application-local.properties`, así que se
lee automáticamente: **no hay que activar ningún perfil**. Si el archivo no existe,
el servicio no arranca (los tests son la excepción: usan H2).

`useSSL=true` no es opcional: la conexión sale de Colombia a un servidor remoto por
internet abierto; sin SSL la contraseña y los datos viajarían en texto plano.

### 4. Ejecutar
```
cd ms-identidad
./mvnw.cmd spring-boot:run
```
Repetir por módulo.

## Pruebas
```
cd <módulo> && ./mvnw.cmd test
```
Usan H2 en memoria (perfil `test`); no tocan Railway y no necesitan
`application-local.properties`.

## Comandos de build
Ver `CLAUDE.md`.
