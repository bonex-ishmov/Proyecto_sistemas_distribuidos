/**
 * Entorno de desarrollo: lo usa `ng serve` (configuración "development" por
 * defecto en angular.json, que reemplaza a environment.ts con este archivo).
 * URLs de los 5 microservicios corriendo en local con `./mvnw.cmd spring-boot:run`.
 */
export const environment = {
  production: false,
  identidadUrl: 'http://localhost:8081',
  clientesUrl: 'http://localhost:8082',
  catalogoUrl: 'http://localhost:8083',
  ventasUrl: 'http://localhost:8084',
  reportesUrl: 'http://localhost:8085',
};
