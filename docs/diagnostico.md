# Diagnóstico técnico — Tienda Genérica

Fecha: 2026-08-27
Alcance: monorepo `Proyecto_sistemas_distribuidos` (ms-identidad, ms-clientes, ms-catalogo)
contrastado contra `docs/requisitos-tienda-generica.pdf` (Parte 1, 7 módulos).

## 1. ARQUITECTURA ACTUAL

Monorepo con 3 microservicios Spring Boot independientes, sin `pom.xml` padre. Cada
uno con su wrapper Maven, su esquema MySQL y su puerto.

| Módulo | Puerto | Esquema | Paquete raíz |
|---|---|---|---|
| ms-identidad | 8081 | db_identidad | co.edu.tienda.identidad |
| ms-clientes | 8082 | db_clientes | co.edu.tienda.clientes |
| ms-catalogo | 8083 | db_catalogo | co.edu.tienda.catalogo |

- Stack: Spring Boot 4.1.0, Java 25, Spring Data JPA + Hibernate (ddl-auto=update),
  Spring Security stateless, MySQL 8 (localhost:3306), JJWT 0.12.6, OpenCSV 5.9
  (solo catálogo). JUnit 5 + Mockito.
- Capas por módulo: Controller (/api/...) -> Service (@Transactional, reglas) ->
  Repository (Spring Data). DTOs son records; mappers son clases utilitarias
  estáticas; GlobalExceptionHandler (@RestControllerAdvice) centraliza los errores
  en un ErrorResponse uniforme.
- Autenticación: secreto HS256 compartido (mismo literal en los 3
  application.properties). Solo ms-identidad emite tokens; los otros dos solo
  verifican la firma con copias casi verbatim de JwtService + JwtAuthenticationFilter.
  Sin roles, sin consulta a BD en la validación.
- Sin FKs entre esquemas. Única FK real: Producto -> Proveedor
  (fk_producto_proveedor) dentro de db_catalogo.
- No hay: frontend, API gateway, service registry, docker-compose, CI,
  documentación OpenAPI. README.md está vacío (solo el título). .mcp.json (config
  del MCP de IntelliJ) está sin trackear.

## 2. MICROSERVICIOS EXISTENTES

ms-identidad — Login + gestión de usuarios.
- POST /api/auth/login -> LoginResponse (token, tipo Bearer, cédula, usuario,
  nombre, expiración).
- GET/POST /api/usuarios, GET/PUT/DELETE /api/usuarios/{cedula},
  PATCH /api/usuarios/{cedula}/estado.
- DataInitializer siembra admininicial / admin123456 (cédula 1) al arrancar,
  idempotente.
- Regla A-4: al crear el primer usuario real, admininicial se marca activo=false
  automáticamente. El login rechaza usuarios inactivos.
- No permite eliminar el usuario inicial (solo desactivar).
- Contraseñas con BCrypt; ningún endpoint devuelve password ni hash.

ms-clientes — CRUD de clientes.
- GET/POST /api/clientes, GET/PUT/DELETE /api/clientes/{cedula},
  POST /api/clientes/lote (recibe lista de cédulas, devuelve los que existen;
  pensado para ms-reportes).
- Toda ruta exige JWT.

ms-catalogo — Proveedores + productos + carga CSV.
- GET/POST /api/proveedores, GET/PUT/DELETE /api/proveedores/{nit}. Borrado
  bloqueado si el proveedor tiene productos.
- GET /api/productos, GET /api/productos/{codigo}, POST /api/productos/lote,
  POST /api/productos/carga (multipart, parte "archivo").
- Carga CSV: valida extensión .csv + nº de columnas, autodetecta cabecera (1a celda
  no numérica), valida todas las filas acumulando errores (hasta 25), verifica NITs
  contra BD en una consulta, y si todo está bien reemplaza la tabla entera
  (deleteAllInBatch + saveAll) dentro de una transacción. Mensajes de error
  calcados del PDF.
- setup-db.sql crea db_catalogo y siembra los 5 proveedores que referencia
  productos.csv.

Pendientes (mencionados en CLAUDE.md, no existen): ms-ventas, ms-reportes.

## 3. BACKEND

- Consistencia alta entre módulos: misma estructura de paquetes, mismo estilo de
  excepciones, mismos nombres de columnas snake_case, dominio en español.
- Mapa de errores: RecursoNoEncontradoException->404, ReglaNegocioException->409,
  bean-validation->400, CredencialesInvalidasException->401 (solo identidad),
  ArchivoInvalidoException->400 y DatosInvalidosException->422 (solo catálogo),
  MaxUploadSizeExceededException->413 (catálogo).
- Validación: DTOs *Request con @NotNull/@NotBlank/@Email/@Size/@Pattern/@Positive.
  *UpdateRequest sin cédula/NIT (van en la URL, son PK). En usuarios, password
  opcional en update (si viene vacío se conserva).
- Transaccionalidad: correcta. Lecturas con readOnly=true; la carga CSV y el
  reemplazo de catálogo en una sola transacción.
- JWT: JwtService firma con subject=usuario, claims cedula y nombre, TTL 1h. El
  filtro pone la cédula en auth.setDetails(...) pero ningún controlador la usa
  todavía.
- Tests: solo capa service, unitarios con Mockito, sin contexto Spring. Cobertura
  razonable de reglas. No hay tests de controller, de seguridad, ni de integración.
  Los @SpringBootTest por defecto (Ms*ApplicationTests) sí intentan levantar
  contexto y conectar a MySQL.

## 4. FRONTEND

No existe. No hay ningún proyecto ni archivo de frontend en el repo (ni JSP, ni
React/Vue/Angular, ni HTML/JS suelto). El PDF, en cambio:
- Tiene una sección "Interfaz Gráfica" en cada uno de los 5 sprints (mockups).
- El módulo 7 exige explícitamente que las consultas sean "por pantalla".
- La Parte 4 describe un frontend JSP desplegado como .WAR en Tomcat — excluido por
  decisión de CLAUDE.md.

La capa de presentación está sin definir.

## 5. BASE DE DATOS

Tres esquemas independientes, tablas creadas por Hibernate al arrancar.

db_identidad.usuarios — cedula_usuario (PK, BIGINT), nombre_usuario (100, NN),
email_usuario (100, NN, UNIQUE), usuario (50, NN, UNIQUE), password (255, NN, hash
BCrypt), activo (BOOLEAN, NN).

db_clientes.clientes — cedula_cliente (PK), nombre_cliente (100, NN),
direccion_cliente (150, NN), telefono_cliente (20, NN), email_cliente (100, NN, no
UNIQUE).

db_catalogo.proveedores — nitproveedor (PK, BIGINT), nombre_proveedor (100),
direccion_proveedor (150), telefono_proveedor (20), ciudad_proveedor (60). Todos NN.

db_catalogo.productos — codigo_producto (PK, BIGINT, no autogenerado),
nombre_producto (50, NN), nitproveedor (FK -> proveedores, NN, EAGER), precio_compra
DECIMAL(15,2), ivacompra DECIMAL(5,2) (porcentaje, no monto), precio_venta
DECIMAL(15,2). Índice idx_producto_nitproveedor.

Config MySQL: ms-identidad y ms-clientes traen username=root / password vacío;
ms-catalogo trae el placeholder TU_PASSWORD_DE_MYSQL (no arranca sin editar). Setup
de BD inconsistente entre módulos.

Faltan: ventas y detalleVentas (módulo 6).

## 6. FUNCIONALIDADES YA IMPLEMENTADAS

- Login con validación usuario/contraseña, usuario inicial sembrado, emisión de
  JWT, rechazo de inactivos y mensaje de error del PDF.
- Usuarios: crear, consultar por cédula, listar, actualizar (con password
  opcional), eliminar (protegiendo al inicial), cambiar estado. Duplicados de
  cédula/usuario/email controlados.
- Clientes: crear, consultar, listar, actualizar, eliminar + consulta por lote.
- Proveedores: crear, consultar, listar, actualizar, eliminar (con bloqueo por
  productos asociados).
- Productos: carga masiva CSV con reemplazo total transaccional y validación
  exhaustiva; listar; consultar por código; consulta por lote.
- Seguridad transversal: todas las rutas salvo /api/auth/** exigen JWT válido;
  firma compartida entre los 3 servicios.
- Manejo de errores uniforme y pruebas unitarias de la capa de servicio en los 3
  módulos.

## 7. FUNCIONALIDADES FALTANTES (contra los 7 módulos de la Parte 1)

| # | Módulo (PDF) | Estado | Detalle |
|---|---|---|---|
| 1 | Login | Completo (backend) | Falta la pantalla de login. |
| 2 | Gestión de usuarios | Completo (backend) | CRUD + estado listos. Falta UI. |
| 3 | Gestión de clientes | Completo (backend) | CRUD + lote listos. Falta UI. |
| 4 | Gestión de proveedores | Completo (backend) | CRUD listo. Falta UI. |
| 5 | Gestión de productos | Completo (backend) | Carga CSV + consultas. El PDF solo exige carga por archivo. Falta UI. |
| 6 | Gestión de ventas | No existe | No hay ms-ventas. Falta: tablas ventas + detalleVentas, consecutivo, búsqueda de cliente por cédula, agregado de productos por código, cálculo por producto, total IVA, total con IVA, validación cantidad > 0, persistencia cabecera + detalle, mensaje de confirmación. Requiere validar cédulas contra ms-clientes y ms-identidad por HTTP. |
| 7 | Consultas y reportes | Parcial | a) Listado de usuarios -> GET /api/usuarios. b) Listado de clientes -> GET /api/clientes. c) Total de ventas por cliente -> imposible hoy (no hay ventas). No existe ms-reportes ni la presentación "por pantalla". |

Además falta, a nivel de sistema:
- Frontend completo (todos los sprints tienen "Interfaz Gráfica"; módulo 7 exige
  "por pantalla").
- Endpoint POST /api/usuarios/lote en ms-identidad (sí existen los de clientes y
  productos).
- Mecanismo de autenticación servicio-a-servicio para que ms-ventas/ms-reportes
  llamen a los /lote.
- Documentación de API (OpenAPI/Swagger), docker-compose para MySQL, README útil,
  CI.

## 8. PROBLEMAS O RIESGOS DETECTADOS

Inconsistencias entidades Java <-> documento

1. precio_venta / IVA — contradicción con los datos de muestra. CLAUDE.md decide
   que precio_venta NO incluye IVA y que el IVA se calcula sobre él. Pero en
   productos.csv y en la tabla del PDF, precio_venta ~= precio_compra * (1 +
   ivacompra/100). Hay que resolver esto antes de ms-ventas porque define la
   fórmula del total con IVA.
2. Dinero como DECIMAL/BigDecimal en vez de DOUBLE (PDF). Decisión aprobada; se
   registra como desviación deliberada.
3. ivacompra como porcentaje en columna DECIMAL(5,2) validada 0-100. El PDF lo tipa
   DOUBLE sin aclarar si es tasa o monto; la implementación asume tasa.
4. usuarios.activo es una columna que el PDF no menciona (viene de la decisión
   A-4). No es un problema pero un revisor la verá "de más".
5. clientes.email_cliente no es único ni se valida contra duplicados al crear (solo
   la cédula). El PDF no exige unicidad -> correcto, pero es asimétrico respecto a
   usuarios.
6. Nombres de tabla del módulo de ventas: el PDF pide detalleVentas (camelCase).
   Al crearlo hay que decidir el nombre físico y anotarlo.

Riesgos técnicos / de configuración

7. Secreto JWT hardcodeado y versionado en los 3 application.properties (mismo
   literal). Debería salir a variable de entorno / archivo no versionado.
8. JwtService + JwtAuthenticationFilter duplicados verbatim en 3 (pronto 5)
   módulos. Riesgo de divergencia.
9. Setup local inconsistente: ms-catalogo no arranca sin editar el placeholder de
   password; los otros usan password vacío.
10. Los @SpringBootTest (Ms*ApplicationTests) requieren MySQL levantado. ./mvnw
    clean package corre tests y esos context-load tests fallarán sin BD.
11. Sin roles. Cualquier usuario autenticado puede crear/borrar usuarios o cargar
    el catálogo. Puede ser decisión consciente pero no está registrada.
12. admininicial tiene cédula 1; si un usuario real llegara con cédula 1 habría
    colisión de PK (improbable).
13. Validación CSV por extensión, no por contenido. Se acepta cualquier archivo
    llamado .csv.
14. README.md vacío y sin instrucciones de arranque fuera de CLAUDE.md.

## 9. PROPUESTA DE ORDEN DE DESARROLLO

1. Resolver la ambigüedad IVA / precio_venta (sección 8, punto 1) y registrarla en
   "Decisiones ya tomadas". Bloqueante para ventas y reportes.
2. ms-identidad: agregar POST /api/usuarios/lote. Prerequisito de ms-ventas y
   ms-reportes.
3. Definir la autenticación servicio-a-servicio (cómo obtienen token
   ms-ventas/ms-reportes para llamar a los /lote).
4. ms-ventas (módulo 6, puerto sugerido 8084, esquema db_ventas): entidades Venta +
   DetalleVenta, consecutivo, cálculo de totales e IVA, validación de cédulas por
   HTTP, endpoints REST estándar, pruebas de servicio.
5. ms-reportes (módulo 7, puerto 8085): agrega las 3 consultas.
6. Frontend que consuma los 5 servicios: login y los 7 módulos, incluidas las
   consultas "por pantalla".
7. Transversal / cierre: externalizar el secreto JWT, docker-compose de MySQL,
   OpenAPI en cada servicio, README de arranque, evaluar librería compartida para
   la lógica JWT.

## 10. PRÓXIMO PASO

1. Resolver las dos decisiones bloqueantes (semántica IVA; auth servicio-a-servicio).
2. Plan detallado de ms-ventas + el añadido de POST /api/usuarios/lote en
   ms-identidad.
3. Aprobación del plan antes de implementar la Fase 1 (ms-ventas).

---

Actualización 2026-08-27: los puntos abiertos de las secciones 8-10 quedaron
resueltos tras la revisión de este diagnóstico. La semántica IVA / precio_venta se
mantiene (precio_venta es base gravable, el IVA se calcula sobre ella); el frontend
será Angular + TypeScript; no se implementan roles; la autenticación
servicio-a-servicio se hace con un usuario de sistema que hace login contra
ms-identidad, sin API Gateway. La redacción vigente de cada decisión, con su
razonamiento, está en CLAUDE.md ("Decisiones ya tomadas"). Este documento se
conserva como foto del estado inicial del repositorio.

---

Actualización 2026-08-30 (Incremento 1 de ms-ventas):

- Deuda técnica JWT: con ms-ventas, la lógica de validación de token
  (`JwtService` de solo verificación + `JwtAuthenticationFilter` +
  `JwtAuthenticationEntryPoint` + `SecurityConfig`) queda COPIADA VERBATIM en 4
  módulos (ms-identidad la tiene además con el método de firma). Con ms-reportes
  serán 5. Sigue siendo decisión consciente (evita montar una librería compartida
  en un proyecto académico), pero el riesgo de divergencia crece: cualquier
  cambio en la auth hay que replicarlo N veces. Antes de sumar la quinta copia
  (ms-reportes), evaluar extraer un módulo Maven común `tienda-seguridad-jwt`.

- Pendiente de limpiar (arrastrado, no introducido por ms-ventas): los
  `application-local.properties.example` de ms-clientes y ms-catalogo traen la
  contraseña real de Railway y el secreto JWT en texto literal, y esos archivos
  SÍ se versionan. El de ms-ventas se creó ya con placeholders
  (`PON_AQUI_LA_CLAVE`, `PON_AQUI_EL_SECRETO`). Hay que reemplazar los literales
  por placeholders en los otros dos módulos y rotar esas credenciales.

---

Actualización 2026-09-01 (Incremento 4 de ms-reportes — cierre del módulo):

- Deuda técnica JWT: con ms-reportes construido, la copia verbatim
  (`JwtService` de solo verificación + `JwtAuthenticationFilter` +
  `JwtAuthenticationEntryPoint` + `SecurityConfig`) ya está en **5 módulos**
  (ms-identidad, ms-clientes, ms-catalogo, ms-ventas, ms-reportes; ms-identidad
  además tiene el método de firma). Se evaluó extraerla a un módulo Maven común
  (`tienda-seguridad-jwt`) antes de sumar esta quinta copia, como quedó anotado
  en la actualización del 2026-08-30, y se decidió **posponerlo**, no
  descartarlo:
  - Razón arquitectónica: CLAUDE.md fija como decisión de diseño que el repo
    **no tiene `pom.xml` padre/agregador** — cada módulo es un proyecto Maven
    standalone con su propio wrapper, ejecutable de forma independiente.
    Compartir una librería Java entre módulos standalone exige una de dos
    cosas: (a) introducir un pom agregador (contradice esa decisión), o (b)
    publicar el módulo compartido a un repositorio Maven local o remoto
    (agrega infraestructura: coordinar versión, y que cada integrante lo
    instale/actualice antes de poder compilar).
  - Razón de alcance: para un proyecto académico, el costo de (a) o (b) no se
    justifica todavía frente al tamaño de lo duplicado (4 clases,
    ~150 líneas por copia) y a que la lógica de verificación no ha cambiado
    desde ms-identidad.
  - Esto NO es indefinido: si aparece un sexto módulo, o si hay que cambiar la
    lógica de validación del JWT y hay que tocar las 5 copias a mano, el
    costo de seguir duplicando supera al de resolver (a) o (b), y ahí sí
    conviene extraer el módulo compartido.

---

Actualización 2026-09-01 (Incremento 0 del frontend — cimientos Angular):

- Deuda técnica CORS: al agregar el frontend (`localhost:4200`) apareció el mismo
  problema de duplicación que el del stack JWT, y por la misma razón (sin `pom.xml`
  agregador, no hay dónde poner una librería compartida). Cada `SecurityConfig`
  ganó un bean `corsConfigurationSource` **copiado verbatim en los 5 módulos**
  (origen permitido configurable por `app.cors.allowed-origins` /
  `CORS_ALLOWED_ORIGINS`, métodos GET/POST/PUT/PATCH/DELETE/OPTIONS, headers
  `Authorization` y `Content-Type`), más una línea `.cors(Customizer.withDefaults())`
  agregada a cada cadena de filtros. Mismo diagnóstico que la nota de arriba: se
  pospone la extracción a un módulo compartido por la misma razón arquitectónica y
  de alcance, y con el mismo disparador para revisarlo (un sexto módulo, o tener
  que tocar las 5 copias a mano por un cambio real de política CORS).

---

Actualización 2026-11-19 (prueba integral del despliegue Docker en local):

Estado: los cinco microservicios, el frontend Angular (las 7 pantallas + dashboard)
y el despliegue distribuido de `docker-compose.yml` (19 contenedores: 2 réplicas
por servicio y por front, cada par detrás de su NGINX) están construidos. Varias
notas de arriba quedaron superadas: el esquema ya no lo crea Hibernate
(`ddl-auto=validate` + `db/schema.sql`), MySQL corre en un contenedor local
(Railway se eliminó y sus credenciales se rotaron), los secretos viven en `.env` /
`application-local.properties` (no versionados) y el README documenta el arranque.

Qué se probó y resultado:

- `./mvnw test` en los 5 módulos: 88 tests, 0 fallos.
- `docker compose build` + arranque en frío con volumen vacío
  (`docker compose -p tienda-prueba up -d --wait`): 19 contenedores sanos en ~35 s,
  sin reinicios. Los scripts de `db/` crearon 4 esquemas / 6 tablas, los 5
  proveedores y los permisos de `tienda` (solo SELECT/INSERT/UPDATE/DELETE).
  `ms-identidad` sembró `admininicial` y el usuario de sistema sin chocar entre réplicas.
- Flujo funcional completo por `http://localhost` (42 comprobaciones): login y 401,
  desactivación de `admininicial`, CRUD con 400/404/409, CSV inválido rechazado y
  CSV válido (18 productos), venta con IVA 19 % sobre `precio_venta` y
  `cedula_usuario` tomada del JWT, los 4 reportes, y el usuario de sistema excluido
  del reporte de usuarios. Todo correcto.
- Balanceo: los 5 NGINX de servicio reparten 3/3 entre sus réplicas; nginx-front
  alterna entre front-1 y front-2.
- Tolerancia a fallos: con la réplica 1 de los 5 servicios y `front-1` apagadas,
  todo siguió respondiendo 200 (incluido registrar una venta); al encenderlas se
  reincorporaron.
- Prueba de humo con el volumen real (datos existentes): todos los endpoints 200.

Problemas encontrados (NO corregidos, pendientes de decisión):

15. Un NGINX que (re)arranca mientras una de sus réplicas está apagada no arranca.
    `balanceo.conf.template` nombra las réplicas por nombre de contenedor; Docker
    solo resuelve contenedores encendidos y NGINX resuelve al iniciar, así que
    falla con `host not found in upstream` y entra en un ciclo de reinicios. El
    servicio entero responde 502 aunque la otra réplica esté sana (reproducido con
    `docker stop ms-clientes-1 && docker restart nginx-clientes`). Arreglo posible:
    usar en `REPLICA_1/2` las IP fijas que ya asigna el compose (p. ej.
    `172.28.0.21:8080`); con una IP NGINX arranca y marca la réplica caída como fallida.
16. Zona horaria en Docker. `VentaService` guarda `fecha_venta` con
    `LocalDateTime.now()` (zona del JVM) y calcula "hoy" / "mes" del resumen en
    `America/Bogota`; su comentario asume que el JVM corre en Bogotá. En los
    contenedores el JVM corre en UTC (verificado: venta registrada a las 15:21 de
    Bogotá quedó con `fecha_venta` 20:21). Consecuencia deducida del código, no
    observada (la prueba se hizo antes de las 19:00): una venta hecha
    después de las 19:00 queda con fecha del día siguiente y no cuenta en
    "ventas de hoy" del dashboard. Arreglo posible: `TZ=America/Bogota` en las
    variables comunes del compose. Con `./mvnw` en el equipo no ocurre.
17. Aviso menor en el arranque de `ms-ventas` (HV000271): `@Valid` sobre
    `List<LineaVentaRequest>` está deprecado en Hibernate Validator; lo recomendado
    es `List<@Valid LineaVentaRequest>`. Hoy funciona igual (la validación de
    cantidad 0 → 400 se comprobó).

Resolución (mismo día, 2026-11-19):

- Punto 15 corregido: en `docker-compose.yml` los 6 NGINX reciben `REPLICA_1/2` con
  la IP fija de cada réplica (p. ej. `172.28.0.21:8080`) en vez del nombre del
  contenedor. Verificado: con `ms-clientes-1` apagada, `docker restart nginx-clientes`
  arranca sin reinicios y la app sigue respondiendo 200.
- Punto 16 corregido: `TZ: America/Bogota` en las variables comunes de las 10
  réplicas Spring Boot (`x-spring-env`). Verificado: `fecha_venta` queda en hora de
  Bogotá y la venta cuenta en "ventas de hoy". No hizo falta tocar Java.
- Punto 17 sin cambios: es un aviso de deprecación, no un fallo.
- La prueba quedó automatizada en `docker/pruebas/` (`todo.sh`, `e2e.sh` y `fallos.sh`;
  uso en el README). Corrida completa de `todo.sh` después de los arreglos: 88 tests,
  arranque en frío en 34 s, 46/46 comprobaciones de e2e y 30/30 de fallos.

---

Actualización 2026-11-21 (despliegue en la nube: Kubernetes en Oracle Cloud):

Objetivo (pedido de la usuaria, no del PDF): mostrar al profesor la tienda corriendo
en la nube, con Kubernetes gestionando los contenedores, gratis o lo más barato
posible, e infraestructura creada con Terraform.

Decisiones de la usuaria (registradas también en CLAUDE.md, "Decisiones ya tomadas"):
- Proveedor: Oracle Cloud (OKE), cuenta Free Tier, sin pasar a Pay As You Go.
- Los 6 NGINX se reemplazan por Services de Kubernetes (el de entrada, por un Service
  LoadBalancer). Justificación para el profesor: un Service es un nombre y una IP
  estables que reparten entre las réplicas listas; Kubernetes saca del reparto y
  recrea las que mueren, que es lo que NGINX + `restart: on-failure` hacían en Docker.
- MySQL dentro del clúster (StatefulSet + disco persistente), imágenes públicas en
  GHCR, secretos en Secrets de Kubernetes.
- Balanceo visible con la cabecera `X-Atendido-Por-Front` + `kubectl`; 2 nodos, con
  las réplicas repartidas.
- Usar el crédito de prueba (USD 300, vence 2026-12-18) para nodos x86 E5.Flex,
  porque no había capacidad ARM A1 (Always Free) en us-sanjose-1; poder apagar y
  encender los nodos para no gastar crédito.

Qué se construyó:
- Imágenes multi-arquitectura (x86 + ARM) sin emulación: la etapa de compilación de
  los 6 Dockerfile corre en `$BUILDPLATFORM`. `docker/publicar-imagenes.sh` las sube
  a GHCR (`tienda-<módulo>:1.0.0`).
- `frontend/servidor.mjs`: destinos por variables `DESTINO_*` (el compose sigue
  igual), cabecera `X-Atendido-Por-Front`, y una conexión por petición (`agent:
  false`) para que el Service reparta por petición.
- `infra/oci/` (Terraform, 18 recursos): VCN con subred pública para el balanceador
  (solo :80), subred pública para la API de Kubernetes (solo la IP de la usuaria),
  subred privada para los nodos (salida por NAT); clúster OKE Basic; grupo de 2 nodos
  E5.Flex 1 OCPU/8 GB; alerta de presupuesto. Scripts `nodos.sh` (interruptor) y
  `buscar-capacidad-a1.sh`.
- `k8s/` (manifiestos + `desplegar.sh` + `fallos.sh`); `docker/pruebas/e2e.sh` acepta
  `MODO=k8s`.

Verificación (2026-11-19):
- `terraform apply` sin errores; 2 nodos `Ready`; un pod de prueba bajó una imagen de
  GHCR por el NAT; `nodos.sh apagar` y `encender` (≈ 2 min 40 s cada uno) sin que
  Terraform marque diferencias.
- `e2e.sh` en modo k8s contra la IP pública: 53/53 (las 46 funcionales de Docker + 7
  de balanceo: 2 pods de front atendiendo, cada Service con 2 pods listos en nodos
  distintos).
- `k8s/fallos.sh`: 17/17. Con un pod de cada servicio borrado a la vez, 43 de 45
  peticiones respondieron y Kubernetes recreó los 6 pods; borrando `mysql-0`, volvió
  con el mismo disco y los datos intactos.
- 2026-11-21: nodos apagados (0) al terminar la documentación.

Problemas encontrados en el despliegue y corregidos:
18. Los nodos de OKE usan CRI-O, que rechaza nombres de imagen cortos (`mysql:8.4` →
    `ImageInspectError`). Se usan nombres completos (`docker.io/library/...`). El pod
    del StatefulSet hubo que borrarlo a mano: un StatefulSet no reemplaza un pod que
    nunca estuvo listo.
19. Con `whenUnsatisfiable: ScheduleAnyway` las dos réplicas de un servicio llegaron
    a quedar en el mismo nodo (el reparto contaba también los pods de la versión
    anterior). Se pasó a `DoNotSchedule` + `matchLabelKeys: [pod-template-hash]` +
    `nodeTaintsPolicy: Honor`.

Pendientes y riesgos (abiertos):
20. Vencimiento del crédito (2026-12-18): Oracle detendrá los nodos E5. Para seguir
    después hay que migrar a A1 (gratis) cuando haya capacidad.
21. 2 de 45 peticiones fallaron al borrar pods: las que estaban en curso en el pod
    que murió. Mejora posible (no aplicada): una pausa `preStop` de ~5 s para que el
    Service deje de mandarle tráfico antes de apagarlo.
22. La base de la nube tiene el usuario de prueba `ana`/`ana12345` (creado por
    `e2e.sh`, clave publicada en el repo) y `admininicial` desactivado. Hay que crear
    el usuario real de la demo y eliminar `ana`.
23. La IP pública de la usuaria es dinámica: cuando cambia, `kubectl` deja de
    conectar hasta actualizar `mi_ip` y hacer `terraform apply` (pasó el 2026-11-21).
24. La tienda en la nube va por HTTP sin TLS (no hay dominio); el JWT y las claves
    viajan sin cifrar entre el navegador y el balanceador. Aceptable para una demo
    académica, no para datos reales.
25. El Secret se crea desde el mismo `.env` que usa Docker en local: el secreto JWT y
    las claves de MySQL son los mismos en los dos entornos. `k8s/desplegar.sh` acepta
    otro archivo (`k8s/desplegar.sh .env.nube`) si se quieren separar.
26. MySQL tiene una sola réplica: mientras su pod se recrea (y, si cambia de nodo,
    mientras el disco se desconecta y se vuelve a conectar) la tienda no puede leer ni
    escribir datos. En `k8s/fallos.sh` volvió solo con los datos intactos; la duración
    del corte no se midió.
