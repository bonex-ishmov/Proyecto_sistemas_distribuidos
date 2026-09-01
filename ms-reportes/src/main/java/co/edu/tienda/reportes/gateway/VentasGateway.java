package co.edu.tienda.reportes.gateway;

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
}
