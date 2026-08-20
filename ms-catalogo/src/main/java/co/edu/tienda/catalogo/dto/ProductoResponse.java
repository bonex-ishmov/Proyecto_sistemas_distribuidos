package co.edu.tienda.catalogo.dto;

import java.math.BigDecimal;

/**
 * Incluye nombreProveedor aunque no este en la tabla productos:
 * sale de la relacion y le ahorra al frontend una llamada extra.
 */
public record ProductoResponse(
        Long codigoProducto,
        String nombreProducto,
        Long nitproveedor,
        String nombreProveedor,
        BigDecimal precioCompra,
        BigDecimal ivacompra,
        BigDecimal precioVenta
) {}
