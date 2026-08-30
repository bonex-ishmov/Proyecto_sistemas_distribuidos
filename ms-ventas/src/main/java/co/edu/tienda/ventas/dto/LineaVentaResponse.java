package co.edu.tienda.ventas.dto;

import java.math.BigDecimal;

/** Una linea de la venta ya registrada, con sus valores calculados. */
public record LineaVentaResponse(
        Long idDetalle,
        Long codigoProducto,
        String nombreProducto,
        Integer cantidad,
        BigDecimal valorUnitario,
        BigDecimal ivaPorcentaje,
        BigDecimal valorIva,
        BigDecimal valorTotal
) {}
