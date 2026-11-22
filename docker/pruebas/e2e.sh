#!/usr/bin/env bash
# =====================================================================
# docker/pruebas/e2e.sh  --  prueba funcional de punta a punta
#
# Recorre el sistema entrando SOLO por la URL pública, igual que el navegador, y
# comprueba login, usuarios, clientes, catálogo (carga CSV), ventas, reportes,
# el balanceo y la zona horaria de las réplicas. Sirve para los dos despliegues:
#   MODO=docker (por defecto), URL http://localhost:
#     nginx-front -> front-1|2 -> nginx-<servicio> -> ms-<servicio>-1|2 -> mysql
#     El balanceo se ve en la cabecera X-Atendido-Por de cada NGINX.
#   MODO=k8s, URL = IP del balanceador de Oracle (k8s/desplegar.sh la imprime):
#     LoadBalancer -> front ×2 -> Service ms-<servicio> -> 2 pods -> mysql
#     Sin NGINX, el balanceo se comprueba con X-Atendido-Por-Front (qué pod del
#     front atendió) y con kubectl (cada Service con 2 pods listos, en nodos distintos).
#
# NECESITA UNA BASE RECIÉN CREADA: entra con admininicial (que se desactiva al
# crear el primer usuario real) y hace la carga CSV (que BORRA la tabla de
# productos). Si admininicial ya no puede entrar, se detiene sin tocar nada.
# Lo normal es correrlo desde docker/pruebas/todo.sh, que levanta una pila de
# prueba con su propio volumen vacío.
#
# Requiere: bash, curl, jq (y kubectl en MODO=k8s). Uso (desde cualquier carpeta):
#   docker/pruebas/e2e.sh
#   MODO=k8s URL_BASE=http://<IP> docker/pruebas/e2e.sh
# Sale con código 0 si todo pasó y 1 si algo falló.
# =====================================================================
set -u
cd "$(dirname "$0")/../.."          # raíz del repo

MODO=${MODO:-docker}
B=${URL_BASE:-http://localhost}
CSV=ms-catalogo/productos.csv
TMP=$(mktemp -d); trap 'rm -rf "$TMP"' EXIT
CUERPO=$TMP/cuerpo
J='Content-Type: application/json'
OK=0; FALLOS=0

# check <descripción> <esperado> <obtenido>
check() {
  if [ "$2" == "$3" ]; then echo "  OK    $1 ($3)"; OK=$((OK+1))
  else echo "  FALLA $1: esperado '$2', obtenido '$3'"; FALLOS=$((FALLOS+1)); fi
}
# code <args de curl>: deja el cuerpo en $CUERPO e imprime el código HTTP
code() { curl -s -o "$CUERPO" -w '%{http_code}' "$@"; }
# atendido <url> [cabecera]: IP:puerto de la réplica que respondió (cabecera X-Atendido-Por, de NGINX)
atendido() { curl -s -o /dev/null -D - ${2:+-H "$2"} "$1" | grep -i '^x-atendido-por:' | tr -d '\r' | awk '{print $2}'; }
# front_que_atendio <url>: nombre del front que respondió (cabecera de servidor.mjs)
front_que_atendio() { curl -s -o /dev/null -D - "$1" | grep -i '^x-atendido-por-front:' | tr -d '\r' | awk '{print $2}'; }

# ---------------------------------------------------------------------
echo "== 0. Precondición: base recién creada"
if [ "$(code -X POST -H "$J" -d '{"usuario":"admininicial","password":"admin123456"}' $B/api/auth/login)" != 200 ]; then
  echo "  admininicial no puede entrar: la base no está recién creada (o la pila no está arriba)."
  echo "  Esta prueba crea datos y BORRA los productos: en Docker córrela con docker/pruebas/todo.sh;"
  echo "  en Kubernetes, solo sobre un despliegue nuevo (base recién creada)."
  exit 2
fi
echo "  OK    admininicial activo: base vacía"

echo "== 1. Frontend ($MODO: $B)"
check "GET / sirve la app" 200 "$(code $B/)"
grep -q '<app-root' "$CUERPO" && check "index.html contiene <app-root>" si si || check "index.html contiene <app-root>" si no
check "ruta del router /clientes -> index.html" 200 "$(code $B/clientes)"
if [ "$MODO" = k8s ]; then
  # El balanceador de Oracle y el Service reparten sin un turno fijo: con 10
  # peticiones tienen que aparecer los 2 pods del front.
  FRONTS=$(for i in $(seq 1 10); do front_que_atendio $B/; done | sort -u | wc -l)
  check "las peticiones llegan a los 2 pods del front" 2 "$FRONTS"
else
  FRONTS=$(for i in 1 2 3 4; do atendido $B/; done | sort -u | tr '\n' ' ')
  check "nginx-front alterna entre los 2 fronts" "172.28.0.61:4200 172.28.0.62:4200 " "$FRONTS"
fi

echo "== 2. Autenticación (ms-identidad)"
check "sin token -> 401" 401 "$(code $B/api/clientes)"
check "token falso -> 401" 401 "$(code -H 'Authorization: Bearer xxx.yyy.zzz' $B/api/clientes)"
check "clave incorrecta -> 401" 401 "$(code -X POST -H "$J" -d '{"usuario":"admininicial","password":"mala"}' $B/api/auth/login)"
check "login admininicial" 200 "$(code -X POST -H "$J" -d '{"usuario":"admininicial","password":"admin123456"}' $B/api/auth/login)"
A="Authorization: Bearer $(jq -r .token "$CUERPO")"

echo "== 3. Usuarios (ms-identidad)"
USUARIO='{"cedulaUsuario":1010,"nombreUsuario":"Ana Prueba","emailUsuario":"ana@prueba.co","usuario":"ana","password":"ana12345"}'
check "crear primer usuario real" 201 "$(code -X POST -H "$A" -H "$J" -d "$USUARIO" $B/api/usuarios)"
grep -qi password "$CUERPO" && check "la respuesta no trae la contraseña" no si || check "la respuesta no trae la contraseña" no no
check "usuario duplicado -> 409" 409 "$(code -X POST -H "$A" -H "$J" -d "$USUARIO" $B/api/usuarios)"
check "admininicial quedó desactivado -> 401" 401 "$(code -X POST -H "$J" -d '{"usuario":"admininicial","password":"admin123456"}' $B/api/auth/login)"
check "login del usuario nuevo" 200 "$(code -X POST -H "$J" -d '{"usuario":"ana","password":"ana12345"}' $B/api/auth/login)"
A="Authorization: Bearer $(jq -r .token "$CUERPO")"

echo "== 4. Clientes (ms-clientes)"
CLIENTE='{"cedulaCliente":2020,"nombreCliente":"Luis Cliente","direccionCliente":"Calle 1","telefonoCliente":"3001234567","emailCliente":"luis@prueba.co"}'
check "crear cliente" 201 "$(code -X POST -H "$A" -H "$J" -d "$CLIENTE" $B/api/clientes)"
check "cliente duplicado -> 409" 409 "$(code -X POST -H "$A" -H "$J" -d "$CLIENTE" $B/api/clientes)"
check "cliente con datos inválidos -> 400" 400 "$(code -X POST -H "$A" -H "$J" -d '{"cedulaCliente":-1,"nombreCliente":"","direccionCliente":"x","telefonoCliente":"abc","emailCliente":"no"}' $B/api/clientes)"
check "consultar cliente" 200 "$(code -H "$A" $B/api/clientes/2020)"
check "cliente inexistente -> 404" 404 "$(code -H "$A" $B/api/clientes/99999)"

echo "== 5. Catálogo (ms-catalogo)"
check "listar proveedores" 200 "$(code -H "$A" $B/api/proveedores)"
check "proveedores sembrados por db/seed.sql" 5 "$(jq length "$CUERPO")"
printf 'codigo,nombre,nit,pc,iva,pv\n1,X,999,1,19,2\n' > "$TMP/malo.csv"
C=$(code -X POST -H "$A" -F "archivo=@$TMP/malo.csv" $B/api/productos/carga)
[ "$C" -ge 400 ] && [ "$C" -lt 500 ] && check "CSV con NIT inexistente rechazado" 4xx 4xx || check "CSV con NIT inexistente rechazado" 4xx "$C"
check "carga del CSV válido" 200 "$(code -X POST -H "$A" -F "archivo=@$CSV" $B/api/productos/carga)"
check "productos cargados = filas del CSV" "$(( $(grep -c . "$CSV") - 1 ))" "$(curl -s -H "$A" $B/api/productos | jq length)"
check "consultar producto 1" 200 "$(code -H "$A" $B/api/productos/1)"
PV1=$(jq -r .precioVenta "$CUERPO"); PV2=$(curl -s -H "$A" $B/api/productos/2 | jq -r .precioVenta)

echo "== 6. Ventas (ms-ventas -> ms-clientes y ms-catalogo por sus balanceadores)"
check "registrar venta (2 líneas)" 201 "$(code -X POST -H "$A" -H "$J" -d '{"cedulaCliente":2020,"detalles":[{"codigoProducto":1,"cantidad":2},{"codigoProducto":2,"cantidad":3}]}' $B/api/ventas)"
cp "$CUERPO" "$TMP/venta"
# Montos comparados como números, redondeados a 2 decimales (jq hace la cuenta).
# El "+ 0" hace que jq imprima 125349 y no el literal 125349.00 de la respuesta.
TOTAL=$(jq -n "$PV1*2 + $PV2*3 | .*100 | round / 100")
check "total venta = sum(precio_venta x cantidad)" "$TOTAL" "$(jq '.totalVenta + 0' "$TMP/venta")"
check "IVA = 19 % del total" "$(jq -n "$TOTAL*0.19 | .*100 | round / 100")" "$(jq '.valorIva + 0' "$TMP/venta")"
check "total con IVA = total + IVA" "$(jq '.totalVenta + .valorIva | .*100 | round / 100' "$TMP/venta")" "$(jq '.totalConIva + 0' "$TMP/venta")"
check "cédula del vendedor tomada del JWT" 1010 "$(jq -r '.cedulaUsuario' "$TMP/venta")"
check "consultar venta por código" 200 "$(code -H "$A" $B/api/ventas/$(jq -r .codigoVenta "$TMP/venta"))"
check "venta a cliente inexistente -> 404" 404 "$(code -X POST -H "$A" -H "$J" -d '{"cedulaCliente":99999,"detalles":[{"codigoProducto":1,"cantidad":1}]}' $B/api/ventas)"
check "venta con producto inexistente -> 404" 404 "$(code -X POST -H "$A" -H "$J" -d '{"cedulaCliente":2020,"detalles":[{"codigoProducto":99999,"cantidad":1}]}' $B/api/ventas)"
check "venta con cantidad 0 -> 400" 400 "$(code -X POST -H "$A" -H "$J" -d '{"cedulaCliente":2020,"detalles":[{"codigoProducto":1,"cantidad":0}]}' $B/api/ventas)"

echo "== 7. Zona horaria (TZ=America/Bogota en las réplicas)"
# fecha_venta sale del reloj del JVM: debe ser la hora de Bogotá, no UTC.
FECHA=$(jq -r '.fechaVenta' "$TMP/venta" | cut -c1-19)
DIF=$(( $(TZ=America/Bogota date +%s) - $(TZ=America/Bogota date -d "$FECHA" +%s) ))
[ "${DIF#-}" -lt 300 ] && check "fecha_venta en hora de Bogotá" si si || check "fecha_venta en hora de Bogotá ($FECHA, desfase ${DIF}s)" si no
check "la venta cuenta en 'ventas de hoy'" 1 "$(curl -s -H "$A" $B/api/ventas/resumen | jq '.hoy.cantidad')"

echo "== 8. Reportes (ms-reportes -> identidad, clientes, catálogo, ventas)"
check "reporte de usuarios" 200 "$(code -H "$A" $B/api/reportes/usuarios)"
jq -e 'map(.usuario) | index("ventas-sistema")' "$CUERPO" >/dev/null \
  && check "usuario de sistema excluido del reporte" si no || check "usuario de sistema excluido del reporte" si si
check "reporte de clientes" 200 "$(code -H "$A" $B/api/reportes/clientes)"
check "reporte de ventas por cliente" 200 "$(code -H "$A" $B/api/reportes/ventas-por-cliente)"
check "resumen del dashboard" 200 "$(code -H "$A" $B/api/reportes/resumen)"
check "dashboard: productos/clientes/proveedores/usuarios activos/ventas hoy" "18 1 5 1 1" \
  "$(jq -r '"\(.productos) \(.clientes) \(.proveedores) \(.usuariosActivos) \(.ventas.hoy.cantidad)"' "$CUERPO")"

if [ "$MODO" = k8s ]; then
  echo "== 9. Balanceo: cada Service tiene 2 pods listos, en nodos distintos"
  for s in ms-identidad ms-clientes ms-catalogo ms-ventas ms-reportes front; do
    # Los pods listos son los que el Service usa para repartir (sus "endpoints").
    NODOS=$(kubectl -n tienda get pods -l app=$s \
      -o jsonpath='{range .items[?(@.status.containerStatuses[0].ready==true)]}{.spec.nodeName}{"\n"}{end}')
    echo "        $s: $(echo $NODOS)"
    check "Service $s: pods listos" 2 "$(echo "$NODOS" | grep -c .)"
    check "Service $s: en nodos distintos" 2 "$(echo "$NODOS" | sort -u | grep -c .)"
  done
  echo
  echo "RESULTADO e2e: $OK OK, $FALLOS fallas"
  [ "$FALLOS" -eq 0 ]
  exit
fi

echo "== 9. Balanceo: cada NGINX de servicio reparte entre sus 2 réplicas (6 peticiones)"
for par in identidad:8093:/api/usuarios clientes:8091:/api/clientes catalogo:8092:/api/proveedores \
           ventas:8090:/api/ventas/resumen reportes:8094:/api/reportes/clientes; do
  IFS=: read -r s p r <<< "$par"
  REPARTO=$(for i in 1 2 3 4 5 6; do atendido "http://localhost:$p$r" "$A"; done | sort | uniq -c | awk '{printf "%s x%s  ", $2, $1}')
  echo "        nginx-$s: $REPARTO"
  check "nginx-$s usa las 2 réplicas" 2 "$(( $(echo $REPARTO | wc -w) / 2 ))"
done

echo
echo "RESULTADO e2e: $OK OK, $FALLOS fallas"
[ "$FALLOS" -eq 0 ]
