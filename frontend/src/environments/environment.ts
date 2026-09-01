/**
 * Entorno de producción: lo usa `ng build` (configuración "production" por
 * defecto en angular.json). Reemplazar por las URLs reales al desplegar los
 * 5 microservicios — mismo criterio que los PON_AQUI_... de los application-
 * local.properties.example del backend.
 */
export const environment = {
  production: true,
  identidadUrl: 'PON_AQUI_LA_URL_DE_MS_IDENTIDAD',
  clientesUrl: 'PON_AQUI_LA_URL_DE_MS_CLIENTES',
  catalogoUrl: 'PON_AQUI_LA_URL_DE_MS_CATALOGO',
  ventasUrl: 'PON_AQUI_LA_URL_DE_MS_VENTAS',
  reportesUrl: 'PON_AQUI_LA_URL_DE_MS_REPORTES',
};
