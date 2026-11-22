#!/usr/bin/env bash
# =====================================================================
# docker/publicar-imagenes.sh  --  construye y sube las 6 imágenes a GitHub
# Container Registry (GHCR), para x86 (linux/amd64) y ARM (linux/arm64).
#
# Kubernetes en Oracle descarga las imágenes de aquí (k8s/ las referencia como
# ghcr.io/bonex-ishmov/tienda-<modulo>:<version>). Son PÚBLICAS: no llevan
# secretos (application-local.properties está en cada .dockerignore; la
# configuración llega por variables de entorno y Secrets de Kubernetes).
#
# Por qué dos arquitecturas: hoy los nodos son x86 (E5.Flex, pagados con el
# crédito de prueba) y la idea es pasar a ARM (A1.Flex, gratis) cuando Oracle
# tenga capacidad. Con una imagen para cada una, el cambio de nodos no toca nada
# más. No hace falta emular ARM: los Dockerfile compilan en la arquitectura de
# esta máquina ($BUILDPLATFORM) y la etapa final solo copia el resultado.
#
# Antes, una sola vez:
#   1) Token de GitHub (classic) con el permiso write:packages.
#   2) docker login ghcr.io -u <usuario-github>     (pegar el token como clave)
#
# Uso:  docker/publicar-imagenes.sh <version>        p. ej.  docker/publicar-imagenes.sh 1.0.0
# Nunca se sobrescribe una versión ya publicada: para un cambio, versión nueva.
# =====================================================================
set -euo pipefail
cd "$(dirname "$0")/.."              # raíz del repo

VERSION=${1:?Uso: docker/publicar-imagenes.sh <version>   (p. ej. 1.0.0)}
REGISTRO=ghcr.io/bonex-ishmov
REPO=https://github.com/bonex-ishmov/Proyecto_sistemas_distribuidos
PLATAFORMAS=linux/amd64,linux/arm64
BUILDER=tienda

grep -q '"ghcr.io"' ~/.docker/config.json 2>/dev/null \
  || { echo "No hay sesión en ghcr.io. Primero: docker login ghcr.io -u <usuario-github>"; exit 1; }

# Builder de buildx que soporta varias plataformas (el driver por defecto no).
docker buildx inspect "$BUILDER" >/dev/null 2>&1 \
  || docker buildx create --name "$BUILDER" --driver docker-container >/dev/null

for modulo in ms-identidad ms-clientes ms-catalogo ms-ventas ms-reportes frontend; do
  imagen="$REGISTRO/tienda-$modulo:$VERSION"
  if docker buildx imagetools inspect "$imagen" >/dev/null 2>&1; then
    echo "!! $imagen ya existe: no se sobrescribe. Usa otra versión."; exit 1
  fi
  echo "== $imagen"
  # org.opencontainers.image.source enlaza el paquete con el repo en GitHub. Va como
  # label (en cada imagen) y como anotación del índice multi-arquitectura, que es
  # donde GHCR la busca cuando la imagen tiene varias plataformas.
  docker buildx build --builder "$BUILDER" --platform "$PLATAFORMAS" \
    --label "org.opencontainers.image.source=$REPO" \
    --annotation "index:org.opencontainers.image.source=$REPO" \
    -t "$imagen" --push "$modulo"
done

echo
echo "Listo. Si es la primera vez, en GitHub -> tu perfil -> Packages, cambia cada"
echo "paquete tienda-* a Public (Package settings -> Change visibility)."
