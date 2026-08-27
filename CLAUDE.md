# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A monorepo of three **independent** Spring Boot 4.1 / Java 25 microservices for a generic
store ("Tienda Generica"), built for a distributed-systems course. Domain language,
identifiers, and messages are all in Spanish — keep new code consistent with that.

| Module        | Port | Schema        | Responsibility                                              |
|---------------|------|---------------|------------------------------------------------------------|
| `ms-identidad`| 8081 | `db_identidad`| Login + user (`usuario`) CRUD. **Only service that mints JWTs.** |
| `ms-clientes` | 8082 | `db_clientes` | Customer (`cliente`) CRUD.                                  |
| `ms-catalogo` | 8083 | `db_catalogo` | Supplier (`proveedor`) + product (`producto`) CRUD, CSV bulk load. |

There is **no aggregator/parent `pom.xml`** at the repo root. Each module is a standalone
Maven project with its own wrapper. All Maven commands must be run from inside the module directory.

## Commands

Run from a module directory (e.g. `ms-identidad/`). Use `./mvnw.cmd` on Windows, `./mvnw` elsewhere.

```
./mvnw.cmd spring-boot:run                      # run the service
./mvnw.cmd clean package                        # build jar (runs tests)
./mvnw.cmd test                                 # all tests
./mvnw.cmd test -Dtest=UsuarioServiceTest       # one test class
./mvnw.cmd test -Dtest=UsuarioServiceTest#crearUsuario   # one test method
```

Tests are plain JUnit 5 + Mockito unit tests on the `service` layer — they mock repositories
and do **not** load a Spring context, so they run fast and need no database.

## Local setup

- The database is a **shared remote MySQL 9.4 on Railway** (`altaria.proxy.rlwy.net:40105`),
  used by the whole team. Services run with `ddl-auto=validate` and never create or alter tables.
- The schema is versioned: run `db/schema.sql` (creates the 3 schemas + all tables), then
  `db/seed.sql` (seeds the 5 suppliers referenced by `ms-catalogo/productos.csv`).
- Each module reads `spring.datasource.*` and `app.jwt.secret` from a **git-ignored**
  `src/main/resources/application-local.properties` (copy it from the committed
  `application-local.properties.example`, drop the `.example` suffix, fill in real values).
  `application.properties` pulls it in via `spring.config.import=optional:classpath:application-local.properties`
  — no profile to activate; the service will not start if the file is missing (tests use H2 instead).
  The datasource URL must use `useSSL=true`; `app.jwt.secret` must be byte-identical across the three modules.
- Shared-DB caveat: `POST /api/productos/carga` wipes the whole `productos` table — never run it
  while a teammate is working.
- `ms-identidad` seeds an initial admin (`admininicial` / `admin123456`) via `DataInitializer`
  on startup. It is **auto-deactivated** the moment the first real user is created.

## Architecture

### Request flow (every module)
`Controller` (`@RestController`, `/api/...`) → `Service` (`@Service`, `@Transactional`, all
business rules and validation) → `Repository` (Spring Data JPA).

- **DTOs are Java `record`s** (`*Request`, `*Update Request`, `*Response`). Entities are never
  exposed directly.
- **Mappers are static utility classes** (`UsuarioMapper.aResponse(...)`), not Spring beans.
- **`GlobalExceptionHandler`** (`@RestControllerAdvice`) is the single place that turns
  exceptions into the uniform `ErrorResponse` JSON. Throw the domain exceptions and let it map:
  - `RecursoNoEncontradoException` → 404
  - `ReglaNegocioException` → 409 (duplicate keys, "cannot delete initial user", etc.)
  - bean-validation failure → 400
  - `CredencialesInvalidasException` → 401 (`ms-identidad` only)
  - `ms-catalogo` adds `ArchivoInvalidoException` and `DatosInvalidosException` for CSV errors.

### Authentication — shared secret, single issuer
- All three services share the **same HS256 secret** in `app.jwt.secret`
  (`application.properties`). It **must be byte-identical** across modules or token
  verification fails.
- Only `ms-identidad` creates tokens (`JwtService.generarToken` in `AuthService.login`).
  Subject = `usuario`; claims = `cedula`, `nombre`. Default TTL 1h (`app.jwt.expiracion-ms`).
- `JwtAuthenticationFilter` + `JwtService` are **copied almost verbatim into every module**
  (only the package differs). The copies **only verify the signature** — no DB lookup, no
  roles. When changing auth logic, apply the same change to all three copies.
- `SecurityConfig`: stateless session, CSRF disabled. `ms-identidad` permits `/api/auth/**`
  and authenticates everything else; `ms-clientes` and `ms-catalogo` authenticate every request.

### Cross-service conventions
- Services never share a database or use cross-schema foreign keys. Within `ms-catalogo`,
  `Producto` → `Proveedor` **is** a real FK (`fk_producto_proveedor`) because they live in
  the same schema.
- `POST /api/{recurso}/lote` endpoints take a list of IDs and return many records in one
  call — intended for future internal service-to-service reads (`ms-ventas`, `ms-reportes`).

### CSV bulk load (`ms-catalogo`)
`POST /api/productos/carga`, `multipart/form-data`, part name `archivo`. The service reads the
whole file, validates **every** row accumulating all errors, and **aborts without touching the
DB** if any row is invalid. On success it **replaces the entire product table** inside one
transaction (delete-all then insert). Header row is auto-detected (first cell not numeric).

## Documento de requisitos

La fuente funcional del proyecto es `docs/requisitos-tienda-generica.pdf`
(Ing. Ricardo D. Camargo, UEB / MinTIC Ciclo 3). Léelo cuando necesites saber
QUÉ debe hacer el sistema.

Qué se usa y qué no:
- Parte 1 (Especificación Funcional): fuente principal de requisitos.
- Parte 2 (Equipo Scrum) y Parte 3 (Sprints, HU, mockups, casos de prueba): NO son obligatorias.
- Parte 4 (Especificaciones Técnicas): solo lo compatible con la arquitectura actual.
- NO se usan: JSP, Tomcat como arquitectura principal, AWS/EC2/Elastic Beanstalk,
  Eclipse, ni arquitectura monolítica. El stack real es el descrito arriba en este archivo.

El documento define el QUÉ, no el CÓMO.

## Decisiones ya tomadas (no volver a proponerlas ni a decidirlas de otro modo)

Estas decisiones ya fueron discutidas y aprobadas. Varias contradicen el PDF a propósito;
cuando el PDF diga otra cosa, gana lo de aquí:

- Dinero: `DECIMAL` en BD y `BigDecimal` en Java. El PDF dice DOUBLE — no se usa.
- IVA: `precio_venta` es la base gravable y NO incluye IVA; el IVA se calcula
  sobre `precio_venta`. Razón: la pantalla de ventas del PDF define tres campos
  separados (Total Venta, Total IVA, Total con IVA); si `precio_venta` ya
  incluyera el IVA, "Total Venta" y "Total con IVA" serían el mismo valor y el
  formulario no tendría sentido. La relación `precio_venta ≈ precio_compra × 1.19`
  que aparece en `productos.csv` es un artefacto de datos de muestra generados
  sin margen comercial, no una regla de negocio.
- Venta: sin límite de productos en el backend. El "máximo 3 productos" del PDF es
  solo la maqueta; el frontend muestra 3 filas por defecto pero puede agregar más.
- Contraseñas: hasheadas siempre. Ningún endpoint devuelve la contraseña ni el hash,
  aunque el mockup del PDF muestre una columna Password.
- Rutas: convención REST estándar (`GET /api/clientes/{id}`), no las del PDF
  (`/clientes/guardar`, `/clientes/eliminar/{id}`).
- Usuario inicial `admininicial` se desactiva automáticamente al crear el primer
  usuario real (columna `activo`).
- Frontend: una sola aplicación en Angular + TypeScript que consume los servicios
  REST. Los mockups del PDF son referencia visual del QUÉ, no del CÓMO. No se usa
  JSP (ver "Documento de requisitos").
- Roles: NO se implementan. El PDF usa las palabras "administrador" y "usuario" en
  las historias, pero no define tabla de roles, ni gestión de roles, ni permisos
  por módulo; la tabla `usuarios` que especifica solo tiene cédula, nombre, email,
  usuario y contraseña. Implementar roles sería inventar un requisito.
- Autenticación servicio-a-servicio: sin API Gateway. `ms-ventas` y `ms-reportes`
  hacen login contra `ms-identidad` con un usuario de sistema propio y usan ese
  JWT para llamar a los endpoints `/lote`, que siguen exigiendo JWT. El usuario de
  sistema se excluye del listado de usuarios del módulo de reportes.
- Carga CSV de productos: modo reemplazo total de la tabla, en una transacción.
- Versiones: se usa Java y Spring Boot actuales. Las versiones del PDF (Java 11,
  Spring Boot 2.4.5) no obligan.
- Microservicios pendientes por construir: `ms-ventas` y `ms-reportes`.

## Persistencia entre microservicios

El PDF diseña UNA sola base de datos con FKs entre `ventas`, `clientes`, `usuarios`
y `productos`. Esta arquitectura NO hace eso: un esquema por servicio, sin FKs entre
esquemas. Consecuencia para `ms-ventas`: guarda `cedula_cliente` y `cedula_usuario`
como valores sin FK, y valida su existencia llamando por HTTP a `ms-clientes` y
`ms-identidad` (los endpoints `/lote` existen para eso). Esas llamadas HTTP van
autenticadas con el JWT del usuario de sistema (ver "Autenticación
servicio-a-servicio").

## Cómo trabajar conmigo

Proyecto académico. Debo poder entender y explicar cada línea ante mi profesor.

- Avanza por etapas. No pases a la siguiente fase hasta que yo lo indique.
- Antes de implementar, explica: qué se construye, en qué microservicio, qué archivos
  se crean o modifican, dónde van, qué dependencias y qué configuración de MySQL hacen
  falta, cómo se ejecuta y cómo se prueba.
- Analiza antes de modificar. No regeneres archivos existentes; ante un error, corrige
  lo que hay en vez de rehacer. Respeta el código que ya funciona.
- Si hay contradicción o ambigüedad entre el PDF y lo que te pido, señálala y
  pregúntame. No decidas en silencio.
- Diferencia siempre: lo que exige el documento / lo que es decisión técnica / lo que
  es recomendación tuya.
- No inventes requisitos. No agregues patrones ni tecnologías para complejizar.
- No hagas commit ni push a menos que te lo pida.
- No afirmes que algo funciona si no lo comprobaste.
- No modifiques este archivo (CLAUDE.md) sin pedirme permiso primero.