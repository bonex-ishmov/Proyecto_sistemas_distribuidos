package co.edu.tienda.ventas.dto;

/**
 * Cuerpo de GET /api/ventas/resumen: ventas de hoy y del mes en curso.
 * Lo consume ms-reportes para armar el dashboard de inicio del frontend.
 */
public record ResumenVentasResponse(
        ResumenPeriodoResponse hoy,
        ResumenPeriodoResponse mes
) {}
