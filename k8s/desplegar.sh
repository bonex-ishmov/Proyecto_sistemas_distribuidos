#!/usr/bin/env bash
# =====================================================================
# k8s/desplegar.sh  --  despliega (o actualiza) la tienda en el clúster
#
#   1) namespace "tienda"
#   2) Secret tienda-secretos, desde el .env de la raíz (git-ignorado): las
#      claves nunca se escriben en un archivo del repo.
#   3) ConfigMap mysql-init, desde db/*.sql (una sola copia de los scripts: los
#      mismos que usa docker-compose.yml).
#   4) todos los manifiestos de k8s/
#   5) espera a que todo esté listo e imprime la URL pública.
#
# Se puede correr las veces que haga falta: kubectl apply solo cambia lo que difiere.
# Requiere kubectl apuntando al clúster (ver infra/oci: terraform output configurar_kubectl)
# y los nodos encendidos (infra/oci/nodos.sh encender).
#
# Uso:  k8s/desplegar.sh [archivo-env]      (por defecto .env)
# =====================================================================
set -euo pipefail
cd "$(dirname "$0")/.."              # raíz del repo
ENV=${1:-.env}
NS=tienda

[ -f "$ENV" ] || { echo "No existe $ENV (copia .env.example y ponle los valores)"; exit 1; }
for clave in MYSQL_ROOT_PASSWORD MYSQL_PASSWORD JWT_SECRET USUARIO_SISTEMA USUARIO_SISTEMA_PASSWORD; do
  grep -q "^$clave=." "$ENV" || { echo "Falta $clave en $ENV"; exit 1; }
done

echo "== 1. Namespace"
kubectl apply -f k8s/00-namespace.yaml

echo "== 2. Secret tienda-secretos (desde $ENV)"
# --dry-run=client -o yaml arma el objeto sin crearlo; apply --server-side lo
# crea o actualiza sin dejar una copia de los valores en anotaciones del objeto.
kubectl -n $NS create secret generic tienda-secretos --from-env-file="$ENV" --dry-run=client -o yaml \
  | kubectl apply --server-side -f - >/dev/null
echo "secret/tienda-secretos aplicado"

echo "== 3. ConfigMap mysql-init (desde db/)"
# El prefijo numérico fija el orden en que MySQL los ejecuta (igual que en el compose).
kubectl -n $NS create configmap mysql-init \
  --from-file=01-schema.sql=db/schema.sql \
  --from-file=02-seed.sql=db/seed.sql \
  --from-file=03-usuario-app.sql=db/usuario-app.sql \
  --dry-run=client -o yaml | kubectl apply --server-side -f - >/dev/null
echo "configmap/mysql-init aplicado"

echo "== 4. Manifiestos"
kubectl apply -f k8s/

echo "== 5. Esperando a que todo esté listo (el primer arranque tarda unos minutos)"
kubectl -n $NS rollout status statefulset/mysql --timeout=10m
for d in ms-identidad ms-clientes ms-catalogo ms-ventas ms-reportes front; do
  kubectl -n $NS rollout status deployment/$d --timeout=10m
done

echo "== IP pública del balanceador"
for _ in $(seq 1 60); do
  IP=$(kubectl -n $NS get service front -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
  [ -n "$IP" ] && break
  sleep 5
done
kubectl -n $NS get pods -o wide
echo
echo "Tienda en: http://${IP:-<pendiente: kubectl -n tienda get service front>}"
