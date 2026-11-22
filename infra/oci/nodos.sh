#!/usr/bin/env bash
# =====================================================================
# infra/oci/nodos.sh  --  interruptor de los nodos del clúster
#
#   infra/oci/nodos.sh encender   -> 2 nodos (la tienda vuelve a correr)
#   infra/oci/nodos.sh apagar     -> 0 nodos (deja de gastarse crédito en máquinas)
#   infra/oci/nodos.sh estado     -> cuántos nodos hay y en qué estado
#
# Con 0 nodos sigue existiendo lo que es gratis: el clúster OKE Basic, la red, el
# balanceador de 10 Mbps y el disco de MySQL (los datos NO se pierden). Al
# encender, Oracle crea máquinas nuevas y Kubernetes vuelve a poner los pods solo.
#
# Terraform no deshace este cambio: oke.tf ignora el tamaño del grupo de nodos.
# Requiere: oci (CLI), terraform y kubectl configurados.
# =====================================================================
set -euo pipefail
cd "$(dirname "$0")"
export SUPPRESS_LABEL_WARNING=True PYTHONWARNINGS=ignore

GRUPO=$(terraform output -raw node_pool_id 2>/dev/null) \
  || { echo "No hay node_pool_id: ¿ya se hizo terraform apply en infra/oci?"; exit 1; }

tamano() {
  oci ce node-pool get --node-pool-id "$GRUPO" --query 'data."node-config-details".size' --raw-output
}

estado() {
  echo "Nodos pedidos al grupo: $(tamano)"
  oci ce node-pool get --node-pool-id "$GRUPO" \
    --query 'data.nodes[].{nodo:name, estado:"lifecycle-state", ip:"private-ip"}' --output table 2>/dev/null \
    || echo "(ninguna máquina)"
  if command -v kubectl >/dev/null && kubectl get nodes >/dev/null 2>&1; then
    echo; kubectl get nodes -o wide
  fi
}

cambiar() {
  local n=$1
  echo "Cambiando el grupo de nodos a $n nodo(s)..."
  oci ce node-pool update --node-pool-id "$GRUPO" --size "$n" --force \
    --wait-for-state SUCCEEDED --wait-for-state FAILED --max-wait-seconds 1800 \
    --query 'data.status' --raw-output
}

case "${1:-}" in
  encender)
    cambiar 2
    echo "Esperando a que Kubernetes vea los 2 nodos listos (puede tardar unos minutos)..."
    for _ in $(seq 1 60); do
      listos=$(kubectl get nodes --no-headers 2>/dev/null | awk '$2=="Ready"' | wc -l)
      [ "$listos" -ge 2 ] && break
      sleep 15
    done
    estado ;;
  apagar)
    cambiar 0
    estado ;;
  estado)
    estado ;;
  *)
    echo "Uso: $0 encender|apagar|estado"; exit 1 ;;
esac
