package co.edu.tienda.ventas.dto;

import java.math.BigDecimal;

/**
 * Una fila del reporte "total de ventas por cliente" (modulo 7 del PDF).
 *
 * totalConIva = suma de ventas.total_con_iva de todas las ventas de ese cliente.
 * Sin desglose ni conteo: se decidio dejar solo la cedula y el total con IVA.
 */
public record TotalPorClienteResponse(
        Long cedulaCliente,
        BigDecimal totalConIva
) {}
