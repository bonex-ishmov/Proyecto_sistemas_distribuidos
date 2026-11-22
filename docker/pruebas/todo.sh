#!/usr/bin/env bash
# =====================================================================
# docker/pruebas/todo.sh  --  verificación completa del proyecto en local
#
#   1) ./mvnw test en los 5 microservicios.
#   2) docker compose build (las 6 imágenes).
#   3) Arranque en frío: levanta los 19 contenedores como proyecto "tienda-prueba",
#      que tiene SU PROPIO volumen vacío, así que MySQL ejecuta db/*.sql desde cero.
#      El volumen de siempre (con tus datos) no se toca.
#   4) Revisa lo que crearon los scripts de db/ (esquemas, tablas, proveedores, permisos).
#   5) Corre e2e.sh (flujo funcional) y fallos.sh (tolerancia a fallos).
#   6) Borra la pila de prueba y su volumen, y si la pila normal estaba encendida
#      al empezar, la vuelve a encender.
#
# Los nombres de contenedor y los puertos son fijos, así que la pila normal se apaga
# (sin borrar su volumen) mientras dura la prueba.
#
# Requiere: bash, curl, jq, docker con compose, JDK 25 y un .env completo.
# Uso (desde cualquier carpeta):
#   docker/pruebas/todo.sh              # todo
#   docker/pruebas/todo.sh --sin-tests  # se salta el paso 1 (más rápido)
# =====================================================================
set -u
cd "$(dirname "$0")/../.."          # raíz del repo
DIR=docker/pruebas
RESUMEN=()
paso() { echo; echo "######## $*"; }
anotar() { RESUMEN+=("$1: $2"); }

# ---------------------------------------------------------------------
if [ "${1:-}" != "--sin-tests" ]; then
  paso "1. Tests de los 5 microservicios"
  for m in ms-identidad ms-clientes ms-catalogo ms-ventas ms-reportes; do
    if (cd "$m" && ./mvnw -q test >/dev/null 2>&1); then echo "  OK    $m"; anotar "tests $m" OK
    else echo "  FALLA $m (ver $m/target/surefire-reports)"; anotar "tests $m" FALLA; fi
  done
fi

paso "2. Construir imágenes"
docker compose build -q || { echo "Falló la construcción de imágenes"; exit 1; }

# Se recuerda si la pila normal estaba encendida para dejarla igual al final.
NORMAL_ARRIBA=$(docker compose ps -q | wc -l)
paso "3. Arranque en frío (proyecto tienda-prueba, volumen vacío)"
[ "$NORMAL_ARRIBA" -gt 0 ] && { echo "  Apagando la pila normal (sus datos quedan en su volumen)"; docker compose down >/dev/null 2>&1; }
export COMPOSE_PROJECT_NAME=tienda-prueba
# Al salir (bien o mal): borrar la pila de prueba y restaurar la normal si estaba arriba.
limpiar() {
  paso "6. Limpieza"
  COMPOSE_PROJECT_NAME=tienda-prueba docker compose down -v >/dev/null 2>&1 && echo "  Pila de prueba y su volumen borrados"
  if [ "$NORMAL_ARRIBA" -gt 0 ]; then
    env -u COMPOSE_PROJECT_NAME docker compose up -d --wait >/dev/null 2>&1 && echo "  Pila normal encendida de nuevo"
  fi
}
trap limpiar EXIT
docker compose down -v >/dev/null 2>&1      # por si quedó una prueba anterior a medias
INICIO=$(date +%s)
if docker compose up -d --wait >/dev/null 2>&1; then
  echo "  OK    19 contenedores sanos en $(( $(date +%s) - INICIO )) s"; anotar "arranque en frío" OK
else
  echo "  FALLA el arranque:"; docker compose ps -a --format '  {{.Name}}\t{{.Status}}'; anotar "arranque en frío" FALLA; exit 1
fi

paso "4. Lo que crearon los scripts de db/"
set -a; . ./.env; set +a
sql() { docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" tienda-mysql mysql -uroot -N -e "$1"; }
TABLAS=$(sql "select count(*) from information_schema.tables where table_schema in ('db_identidad','db_clientes','db_catalogo','db_ventas')")
PROV=$(sql "select count(*) from db_catalogo.proveedores")
PERMISOS=$(sql "show grants for 'tienda'@'%'" | grep -c 'SELECT, INSERT, UPDATE, DELETE ON `db_')
echo "  tablas=$TABLAS (esperado 6), proveedores=$PROV (esperado 5), esquemas con permiso para 'tienda'=$PERMISOS (esperado 4)"
[ "$TABLAS $PROV $PERMISOS" == "6 5 4" ] && anotar "scripts db/" OK || anotar "scripts db/" FALLA

paso "5a. Prueba funcional (e2e.sh)"
"$DIR/e2e.sh" && anotar "e2e" OK || anotar "e2e" FALLA
paso "5b. Tolerancia a fallos (fallos.sh)"
"$DIR/fallos.sh" && anotar "fallos" OK || anotar "fallos" FALLA

paso "RESUMEN"
printf '  %s\n' "${RESUMEN[@]}"
! printf '%s\n' "${RESUMEN[@]}" | grep -q FALLA
