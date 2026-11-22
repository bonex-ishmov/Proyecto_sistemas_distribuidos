#!/usr/bin/env bash
# =====================================================================
# k8s/fallos.sh  --  tolerancia a fallos de la tienda en Kubernetes
#
#   1) Borra UN pod de cada Deployment (los 5 microservicios y el front) a la vez,
#      mientras se le hacen peticiones a la tienda: el Service deja de mandarle
#      tráfico al pod que muere y lo manda al otro. Después comprueba que
#      Kubernetes recreó solo los pods (vuelven a ser 2 listos por servicio).
#      En Docker esto lo hacían NGINX + `restart: on-failure`.
#   2) Borra el pod de MySQL (1 sola réplica): hay un corte mientras vuelve, pero
#      el StatefulSet lo recrea con el MISMO disco y los datos siguen ahí.
#
# Solo hace lecturas (GET), con el usuario de sistema de .env.
# Requiere: bash, curl, jq, kubectl. Uso:
#   URL_BASE=http://<IP del balanceador> k8s/fallos.sh
# =====================================================================
set -u
cd "$(dirname "$0")/.."              # raíz del repo
set -a; . ./.env; set +a

B=${URL_BASE:?Falta URL_BASE=http://<IP> (k8s/desplegar.sh la imprime)}
NS=tienda
J='Content-Type: application/json'
DEPLOYMENTS="ms-identidad ms-clientes ms-catalogo ms-ventas ms-reportes front"
OK=0; FALLOS=0
check() {
  if [ "$2" == "$3" ]; then echo "  OK    $1 ($3)"; OK=$((OK+1))
  else echo "  FALLA $1: esperado '$2', obtenido '$3'"; FALLOS=$((FALLOS+1)); fi
}
login() { curl -s -X POST -H "$J" -d "{\"usuario\":\"$USUARIO_SISTEMA\",\"password\":\"$USUARIO_SISTEMA_PASSWORD\"}" $B/api/auth/login | jq -r .token; }
listos() { kubectl -n $NS get pods -l app=$1 -o jsonpath='{range .items[?(@.status.containerStatuses[0].ready==true)]}{.metadata.name}{"\n"}{end}' | grep -c .; }

TOKEN=$(login)
[ -n "$TOKEN" ] && [ "$TOKEN" != null ] || { echo "No se pudo hacer login en $B con el usuario de sistema"; exit 2; }
A="Authorization: Bearer $TOKEN"
PRODUCTOS_ANTES=$(curl -s -H "$A" $B/api/productos | jq length)

# ---------------------------------------------------------------------
echo "== 1. Borrar un pod de cada servicio con tráfico en curso"
BORRADOS=""
for d in $DEPLOYMENTS; do
  pod=$(kubectl -n $NS get pods -l app=$d -o jsonpath='{.items[0].metadata.name}')
  BORRADOS="$BORRADOS $pod"
done
echo "  borrando:$BORRADOS"
kubectl -n $NS delete pod $BORRADOS --wait=false >/dev/null
# 30 s de peticiones que cruzan todos los servicios: el resumen del dashboard
# pasa por front -> ms-reportes -> identidad, clientes, catálogo y ventas.
TOTAL=0; MALAS=0
FIN=$(( $(date +%s) + 30 ))
while [ "$(date +%s)" -lt "$FIN" ]; do
  c=$(curl -s -o /dev/null -w '%{http_code}' -m 10 -H "$A" $B/api/reportes/resumen)
  TOTAL=$((TOTAL+1)); [ "$c" = 200 ] || MALAS=$((MALAS+1))
done
echo "  durante la caída: $((TOTAL-MALAS)) de $TOTAL peticiones respondieron 200"
# Puede fallar alguna petición que estaba EN CURSO en el pod justo cuando murió;
# lo que se exige es que la gran mayoría responda (>= 90 %).
[ $(( (TOTAL-MALAS) * 100 / TOTAL )) -ge 90 ] && check "la tienda siguió respondiendo (>= 90 %)" si si \
  || check "la tienda siguió respondiendo (>= 90 %)" si "no ($MALAS fallaron)"

echo "  esperando a que Kubernetes recree los pods..."
for d in $DEPLOYMENTS; do
  kubectl -n $NS rollout status deployment/$d --timeout=5m >/dev/null
  check "$d: de nuevo 2 pods listos" 2 "$(listos $d)"
done
for pod in $BORRADOS; do
  kubectl -n $NS get pod "$pod" >/dev/null 2>&1 && check "$pod ya no existe (se creó otro)" no si || check "$pod ya no existe (se creó otro)" no no
done

# ---------------------------------------------------------------------
echo "== 2. Borrar el pod de MySQL (StatefulSet, 1 réplica, disco persistente)"
DISCO=$(kubectl -n $NS get pvc datos-mysql-0 -o jsonpath='{.spec.volumeName}')
kubectl -n $NS delete pod mysql-0 >/dev/null
echo "  mysql-0 borrado; esperando a que vuelva..."
kubectl -n $NS rollout status statefulset/mysql --timeout=5m >/dev/null
kubectl -n $NS wait --for=condition=Ready pod/mysql-0 --timeout=5m >/dev/null
check "mysql-0 volvió a estar listo" True "$(kubectl -n $NS get pod mysql-0 -o jsonpath='{.status.conditions[?(@.type=="Ready")].status}')"
check "usa el mismo disco" "$DISCO" "$(kubectl -n $NS get pvc datos-mysql-0 -o jsonpath='{.spec.volumeName}')"
# Los microservicios reconectan solos al pool de conexiones; se reintenta un rato.
for _ in $(seq 1 24); do
  PRODUCTOS_DESPUES=$(curl -s -m 10 -H "$A" $B/api/productos | jq length 2>/dev/null)
  [ "$PRODUCTOS_DESPUES" = "$PRODUCTOS_ANTES" ] && break
  sleep 5
done
check "los datos siguen ahí (productos antes = después)" "$PRODUCTOS_ANTES" "${PRODUCTOS_DESPUES:-error}"
check "login sigue funcionando" si "$( [ "$(login)" != null ] && echo si || echo no)"

echo
echo "RESULTADO fallos k8s: $OK OK, $FALLOS fallas"
[ "$FALLOS" -eq 0 ]
