package co.edu.tienda.reportes.gateway;

import java.math.BigDecimal;

/**
 * Lo que ms-ventas devuelve en GET /api/ventas/total-por-cliente: una fila por
 * cliente con al menos una venta. Campos idénticos a TotalPorClienteResponse de
 * ms-ventas, así que este record se usa tal cual para deserializar la respuesta.
 */
public record TotalVentasCliente(
        Long cedulaCliente,
        BigDecimal totalConIva
) {}
