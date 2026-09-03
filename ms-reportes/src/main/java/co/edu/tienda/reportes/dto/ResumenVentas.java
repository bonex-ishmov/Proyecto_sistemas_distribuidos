package co.edu.tienda.reportes.dto;

import java.math.BigDecimal;

/**
 * Resumen de ventas de hoy y del mes en curso.
 *
 * Copia local del ResumenVentasResponse de ms-ventas (GET /api/ventas/resumen):
 * este record sirve a la vez para deserializar esa respuesta y como parte del
 * cuerpo de GET /api/reportes/resumen. Mismo criterio que
 * {@link co.edu.tienda.reportes.gateway.TotalVentasCliente}.
 */
public record ResumenVentas(
        Periodo hoy,
        Periodo mes
) {
    /** cantidad de ventas y suma de total_con_iva del periodo. */
    public record Periodo(
            long cantidad,
            BigDecimal montoTotalConIva
    ) {}
}
