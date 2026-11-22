#!/usr/bin/env bash
# =====================================================================
# docker/pruebas/fallos.sh  --  prueba de tolerancia a fallos
#
#   1) Apaga la réplica 1 de los 5 microservicios y front-1 y comprueba que la
#      app sigue respondiendo por http://localhost (NGINX pasa todo a la réplica 2),
#      incluido el resumen del dashboard, que cruza 4 servicios.
#   2) Con ms-clientes-1 todavía apagada, REINICIA nginx-clientes y comprueba que
#      arranca y sigue atendiendo. (Antes de usar IPs fijas en REPLICA_1/2, NGINX no
#      arrancaba: "host not found in upstream" -> 502.)
#   3) Enciende las réplicas y comprueba que vuelven a recibir peticiones.
#
# Solo hace lecturas (GET): se puede correr sobre la pila normal sin tocar datos.
# Entra con el usuario de sistema de .env (USUARIO_SISTEMA / USUARIO_SISTEMA_PASSWORD).
# Respeta COMPOSE_PROJECT_NAME (todo.sh lo pone en tienda-prueba).
#
# Requiere: bash, curl, jq, docker. Uso: docker/pruebas/fallos.sh
# =====================================================================
set -u
cd "$(dirname "$0")/../.."          # raíz del repo
set -a; . ./.env; set +a

B=http://localhost
J='Content-Type: application/json'
APAGAR="ms-identidad-1 ms-clientes-1 ms-catalogo-1 ms-ventas-1 ms-reportes-1 front-1"
OK=0; FALLOS=0
check() {
  if [ "$2" == "$3" ]; then echo "  OK    $1 ($3)"; OK=$((OK+1))
  else echo "  FALLA $1: esperado '$2', obtenido '$3'"; FALLOS=$((FALLOS+1)); fi
}
estado() { curl -s -o /dev/null -w '%{http_code}' ${2:+-H "$2"} "$1"; }
atendido() { curl -s -o /dev/null -D - -H "$A" "$1" | grep -i '^x-atendido-por:' | tr -d '\r' | awk '{print $2}'; }
# Pase lo que pase, al salir las réplicas quedan encendidas.
trap 'docker start $APAGAR >/dev/null 2>&1' EXIT

TOKEN=$(curl -s -X POST -H "$J" -d "{\"usuario\":\"$USUARIO_SISTEMA\",\"password\":\"$USUARIO_SISTEMA_PASSWORD\"}" $B/api/auth/login | jq -r .token)
[ -n "$TOKEN" ] && [ "$TOKEN" != null ] || { echo "No se pudo hacer login con el usuario de sistema: ¿está la pila arriba?"; exit 2; }
A="Authorization: Bearer $TOKEN"

echo "== 1. Apagando la réplica 1 de todo: $APAGAR"
docker stop $APAGAR >/dev/null
for i in 1 2 3 4; do
  check "vuelta $i: app (GET /)"              200 "$(estado $B/)"
  check "vuelta $i: login"                    200 "$(curl -s -o /dev/null -w '%{http_code}' -X POST -H "$J" -d "{\"usuario\":\"$USUARIO_SISTEMA\",\"password\":\"$USUARIO_SISTEMA_PASSWORD\"}" $B/api/auth/login)"
  check "vuelta $i: clientes"                 200 "$(estado $B/api/clientes "$A")"
  check "vuelta $i: productos"                200 "$(estado $B/api/productos "$A")"
  check "vuelta $i: resumen ventas"           200 "$(estado $B/api/ventas/resumen "$A")"
  check "vuelta $i: resumen dashboard"        200 "$(estado $B/api/reportes/resumen "$A")"
done
check "nginx-clientes atiende con la réplica 2" 172.28.0.22:8080 "$(atendido http://localhost:8091/api/clientes)"

echo "== 2. Reiniciar nginx-clientes con ms-clientes-1 apagada"
REINICIOS=$(docker inspect -f '{{.RestartCount}}' nginx-clientes)
docker restart nginx-clientes >/dev/null
sleep 5
check "nginx-clientes sigue corriendo" running "$(docker inspect -f '{{.State.Status}}' nginx-clientes)"
check "nginx-clientes no entró en ciclo de reinicios" "$REINICIOS" "$(docker inspect -f '{{.RestartCount}}' nginx-clientes)"
check "clientes por la app tras el reinicio" 200 "$(estado $B/api/clientes "$A")"

echo "== 3. Encendiendo las réplicas"
docker start $APAGAR >/dev/null
docker compose up -d --wait >/dev/null 2>&1
check "las 19 piezas sanas o corriendo" 19 "$(docker compose ps --status running -q | wc -l)"
# NGINX deja de mandar a una réplica caída unos segundos (fail_timeout=10s por defecto).
sleep 11
REPARTO=$(for i in 1 2 3 4 5 6; do atendido http://localhost:8091/api/clientes; done | sort -u | tr '\n' ' ')
check "ms-clientes-1 vuelve a recibir peticiones" "172.28.0.21:8080 172.28.0.22:8080 " "$REPARTO"

echo
echo "RESULTADO fallos: $OK OK, $FALLOS fallas"
[ "$FALLOS" -eq 0 ]
