/**
 * Entorno Docker: lo usa `ng build --configuration docker` (frontend/Dockerfile),
 * la app que sirven front-1 / front-2 (docker-compose.yml de la raíz).
 *
 * URLs vacías a propósito: los servicios arman `${url}/api/...`, así que las
 * peticiones salen RELATIVAS (`/api/clientes`) hacia el mismo origen del que
 * se cargó la página (el NGINX del frontend, puerto 80). De ahí pasan a
 * servidor.mjs, que las reenvía al NGINX balanceador de cada microservicio.
 * Mismo origen = sin CORS en el navegador.
 */
export const environment = {
  production: true,
  identidadUrl: '',
  clientesUrl: '',
  catalogoUrl: '',
  ventasUrl: '',
  reportesUrl: '',
};
