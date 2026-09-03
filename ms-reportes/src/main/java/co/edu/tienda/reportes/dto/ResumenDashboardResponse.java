package co.edu.tienda.reportes.dto;

/**
 * Cuerpo de GET /api/reportes/resumen: todo lo que el dashboard de inicio del
 * frontend necesita, en UNA sola respuesta. ms-reportes lo arma agregando por
 * HTTP a ms-catalogo (productos, proveedores), ms-clientes (clientes),
 * ms-identidad (usuarios activos) y ms-ventas (resumen de ventas).
 *
 * usuariosActivos = usuarios con activo = true, SIN contar el usuario de sistema
 * (mismo criterio que el listado del modulo 7).
 */
public record ResumenDashboardResponse(
        long productos,
        long clientes,
        long proveedores,
        long usuariosActivos,
        ResumenVentas ventas
) {}
