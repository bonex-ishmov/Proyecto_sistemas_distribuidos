#!/usr/bin/env bash
# =====================================================================
# infra/oci/buscar-capacidad-a1.sh  --  avisa cuando haya capacidad ARM (A1)
#
# Los nodos E5.Flex (x86) se pagan con el crédito de prueba, que vence el
# 2026-12-18; los A1.Flex (ARM) son Always Free, pero Oracle se queda sin
# capacidad A1 en us-sanjose-1 a menudo. Este script le pregunta a Oracle cada
# 10 minutos si podría crear un nodo A1 de 1 OCPU / 6 GB (un "informe de
# capacidad": NO crea nada ni cuesta nada) y termina en cuanto haya.
#
# Uso:  infra/oci/buscar-capacidad-a1.sh            (Ctrl+C para cortar)
# Para migrar, en terraform.tfvars:
#   forma_nodo   = "VM.Standard.A1.Flex"
#   memoria_nodo = 6
# y `terraform apply` (reemplaza los nodos; las imágenes de GHCR ya son ARM+x86).
# =====================================================================
set -uo pipefail
cd "$(dirname "$0")"
export SUPPRESS_LABEL_WARNING=True PYTHONWARNINGS=ignore

TENANCY=$(awk -F'"' '/^tenancy_ocid/ {print $2}' terraform.tfvars)
DOMINIO=$(oci iam availability-domain list --compartment-id "$TENANCY" --query 'data[0].name' --raw-output)
FORMA='[{"instanceShape":"VM.Standard.A1.Flex","instanceShapeConfig":{"ocpus":1,"memoryInGBs":6}}]'

while true; do
  estado=$(oci compute compute-capacity-report create --compartment-id "$TENANCY" \
    --availability-domain "$DOMINIO" --shape-availabilities "$FORMA" \
    --query 'data."shape-availabilities"[0]."availability-status"' --raw-output 2>/dev/null || echo "ERROR_CONSULTA")
  echo "$(date '+%F %T')  A1.Flex 1/6: $estado"
  if [ "$estado" = "AVAILABLE" ]; then
    echo "¡Hay capacidad A1! Cambia forma_nodo/memoria_nodo en terraform.tfvars y haz terraform apply."
    command -v notify-send >/dev/null && notify-send "Oracle: hay capacidad A1" "Migrar los nodos de la tienda a ARM"
    exit 0
  fi
  sleep 600
done
