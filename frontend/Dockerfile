# =====================================================================
# frontend/Dockerfile  --  "FR NodeJS" del gráfico (docker-compose.yml)
# Dos etapas: 1) compila la app con `ng build` (configuración docker),
#             2) Node corre servidor.mjs, que sirve lo compilado en el puerto
#                4200 y reenvía /api/* a los microservicios (en Docker, a sus
#                NGINX; en Kubernetes, a sus Service).
# La imagen final no lleva node_modules ni el código fuente: solo los
# archivos compilados y servidor.mjs (que no usa dependencias).
# =====================================================================
# --platform=$BUILDPLATFORM: la compilación corre en la arquitectura de la máquina
# que construye; el resultado (HTML/JS/CSS) es igual para x86 y ARM.
FROM --platform=$BUILDPLATFORM node:24-alpine AS compilacion
WORKDIR /app
# Primero solo los manifiestos: las dependencias quedan en una capa propia y
# no se reinstalan cuando únicamente cambia el código.
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npx ng build --configuration docker

# Sin --platform: variante de la arquitectura destino (amd64 o arm64).
FROM node:24-alpine
WORKDIR /app
COPY --from=compilacion /app/dist/frontend/browser dist/frontend/browser
COPY servidor.mjs .
EXPOSE 4200
CMD ["node", "servidor.mjs"]
