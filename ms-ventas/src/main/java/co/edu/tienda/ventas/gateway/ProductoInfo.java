package co.edu.tienda.ventas.gateway;

import java.math.BigDecimal;

/**
 * Datos de un producto que ms-ventas necesita para registrar una venta.
 * Es un subconjunto de lo que expone ms-catalogo (POST /api/productos/lote):
 * solo lo que entra en el calculo o se congela como snapshot en el detalle.
 *
 * - precioVenta  -> se copia a detalle_ventas.valor_unitario (base gravable, SIN IVA)
 * - ivaPorcentaje -> se copia a detalle_ventas.iva_porcentaje (ej. 19.00, NO un monto)
 * - nombre       -> se congela en detalle_ventas.nombre_producto
 */
public record ProductoInfo(
        Long codigo,
        String nombre,
        BigDecimal precioVenta,
        BigDecimal ivaPorcentaje
) {}
