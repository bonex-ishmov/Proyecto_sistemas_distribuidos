# frontend — Tienda Genérica

Aplicación Angular (standalone, zoneless) que consume los 5 microservicios del
repo. Arquitectura y decisiones en `../CLAUDE.md`; plan de incrementos del
frontend en la conversación del Incremento 0.

## Requisitos

- Node 24 / Angular CLI 22 (`ng version` para confirmar).
- Los 5 microservicios backend corriendo en local (puertos 8081-8085) con CORS
  habilitado para `http://localhost:4200` (`app.cors.allowed-origins` en cada
  `application.properties` — ya viene así por defecto).

## Comandos

```bash
npm install       # una vez
npm start         # ng serve, http://localhost:4200, recarga en caliente
npm run build     # build de producción en dist/
npm test          # unit tests (Vitest)
```

`npm start` usa la configuración "development" de `angular.json`, que reemplaza
`src/environments/environment.ts` por `environment.development.ts`
(URLs `http://localhost:808X` de los 5 servicios). El build de producción
(`npm run build`) usa `environment.ts` tal cual — hay que completar sus
`PON_AQUI_LA_URL_DE_...` con las URLs reales antes de desplegar.

## Estructura

- `src/styles.css`: hoja de estilos global (Incremento 7) — tokens de color/
  espaciado y las clases que usan todas las pantallas (`.table`, `.form`,
  `.btn`, `.alert`, `.badge`, `.loading`, `.empty-state`). Los `.css` de cada
  componente solo traen lo que de verdad es propio de esa pantalla.
- `src/app/core/`: infraestructura transversal — `error.interceptor` (banner
  global + 401 → limpia sesión y redirige a `/login`), `auth.interceptor`
  (adjunta el JWT), `auth.guard`, `sesion.service` (JWT en `localStorage`,
  ver CLAUDE.md), `auth.service` (login/logout), `notificacion.service`.
- `src/app/auth/login/`, `home/`: login (módulo 1) y placeholder de inicio.
- `src/app/usuarios/`, `clientes/`, `proveedores/`, `productos/`, `ventas/`,
  `reportes/`: una carpeta por módulo del PDF (2 a 7), cada una con su
  `*.service.ts` + `*.model.ts` contra el microservicio correspondiente y sus
  componentes de pantalla (`*-lista`, `*-formulario`). `productos/` no tiene
  formulario propio: el backend solo permite listar y cargar CSV
  (`productos-carga/`), no CRUD individual. `ventas/` no tiene listado: el
  backend no lo expone, solo registrar y consultar por código.
