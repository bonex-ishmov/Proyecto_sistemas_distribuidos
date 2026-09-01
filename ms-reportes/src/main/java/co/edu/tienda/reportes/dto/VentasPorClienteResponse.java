package co.edu.tienda.reportes.dto;

import java.math.BigDecimal;

/**
 * Una fila del reporte "total de ventas por cliente" (módulo 7, consulta c).
 *
 * totalConIva viene de ms-ventas (suma de ventas.total_con_iva del cliente).
 * nombreCliente lo resuelve ms-reportes cruzando la cédula con ms-clientes.
 * Solo aparecen clientes con al menos una venta.
 */
public record VentasPorClienteResponse(
        Long cedulaCliente,
        String nombreCliente,
        BigDecimal totalConIva
) {}
