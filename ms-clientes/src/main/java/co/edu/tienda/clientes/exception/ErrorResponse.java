package co.edu.tienda.clientes.exception;

import java.time.LocalDateTime;

/**
 * Formato único de error para TODOS los microservicios del proyecto.
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String mensaje,
        String path
) {}