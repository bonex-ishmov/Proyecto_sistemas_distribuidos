package co.edu.tienda.ventas.exception;

import java.time.LocalDateTime;

/** Formato unico de error para TODOS los microservicios del proyecto. */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String mensaje,
        String path
) {}