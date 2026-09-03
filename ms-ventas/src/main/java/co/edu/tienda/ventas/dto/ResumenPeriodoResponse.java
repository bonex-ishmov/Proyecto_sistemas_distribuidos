package co.edu.tienda.ventas.dto;

import java.math.BigDecimal;

/**
 * Resumen de ventas de un periodo (hoy o mes en curso) para el dashboard de
 * inicio.
 *
 * cantidad          = numero de ventas registradas en el periodo.
 * montoTotalConIva  = suma de ventas.total_con_iva de esas ventas (con IVA).
 *
 * La agregacion la hace la BD (COUNT + SUM); si no hay ventas, cantidad = 0 y
 * montoTotalConIva = 0.00.
 */
public record ResumenPeriodoResponse(
        long cantidad,
        BigDecimal montoTotalConIva
) {}
