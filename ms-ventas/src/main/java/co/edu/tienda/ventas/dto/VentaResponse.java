package co.edu.tienda.ventas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta de una venta registrada. Reproduce los tres totales de la pantalla
 * de ventas del PDF:
 *  - totalVenta   = "Total Venta"     (base gravable, SIN IVA)
 *  - valorIva     = "Total IVA"
 *  - totalConIva  = "Total con IVA"   = totalVenta + valorIva
 */
public record VentaResponse(
        Long codigoVenta,
        Long cedulaCliente,
        Long cedulaUsuario,
        BigDecimal totalVenta,
        BigDecimal valorIva,
        BigDecimal totalConIva,
        LocalDateTime fechaVenta,
        List<LineaVentaResponse> detalles
) {}
