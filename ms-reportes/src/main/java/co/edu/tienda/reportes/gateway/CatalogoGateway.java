package co.edu.tienda.reportes.gateway;

/**
 * Puerto hacia ms-catalogo. La implementacion real (HTTP) es
 * {@link CatalogoHttpAdapter}; el contrato queda aparte para poder testear
 * ReporteService con un mock.
 *
 * ms-catalogo no expone un endpoint de conteo, asi que el adaptador trae la
 * lista completa (GET /api/productos, GET /api/proveedores) y la cuenta. Es
 * aceptable para el volumen del proyecto y solo se usa en el dashboard.
 */
public interface CatalogoGateway {

    /** Numero de productos registrados en ms-catalogo. */
    long contarProductos();

    /** Numero de proveedores registrados en ms-catalogo. */
    long contarProveedores();
}
