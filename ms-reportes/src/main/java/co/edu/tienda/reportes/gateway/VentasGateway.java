package co.edu.tienda.reportes.gateway;

import co.edu.tienda.reportes.dto.ResumenVentas;

import java.util.List;

/**
 * Puerto hacia ms-ventas. La implementación real (HTTP) es
 * {@link VentasHttpAdapter}; el contrato queda aparte para poder testear
 * ReporteService con un mock.
 */
public interface VentasGateway {

    /**
     * Total con IVA acumulado por cliente (GET /api/ventas/total-por-cliente).
     * Solo trae clientes con al menos una venta; lista vacía si aún no hay ventas.
     */
    List<TotalVentasCliente> totalPorCliente();

    /**
     * Resumen de ventas de hoy y del mes en curso (GET /api/ventas/resumen),
     * para el dashboard de inicio.
     */
    ResumenVentas resumen();
}
