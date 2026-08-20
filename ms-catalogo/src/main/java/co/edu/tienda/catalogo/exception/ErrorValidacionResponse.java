package co.edu.tienda.catalogo.exception;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Variante de ErrorResponse que ademas transporta el detalle linea por linea.
 * La usa la carga de CSV para reportar todos los errores del archivo.
 */
public record ErrorValidacionResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String mensaje,
        String path,
        List<String> errores
) {}
