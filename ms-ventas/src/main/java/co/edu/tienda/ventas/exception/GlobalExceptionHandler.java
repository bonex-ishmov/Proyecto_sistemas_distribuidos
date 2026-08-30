package co.edu.tienda.ventas.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Captura las excepciones de todos los controladores y las convierte
 * en respuestas HTTP con el formato uniforme ErrorResponse.
 *
 * IMPORTANTE: el handler de Exception.class debe quedar SIEMPRE de ultimo.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> manejarNoEncontrado(
            RecursoNoEncontradoException ex, HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "NOT_FOUND",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponse> manejarReglaNegocio(
            ReglaNegocioException ex, HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "CONFLICT",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Una dependencia (ms-clientes, ms-catalogo, ms-identidad) no esta
     * disponible. La venta no se registro.
     */
    @ExceptionHandler(DependenciaNoDisponibleException.class)
    public ResponseEntity<ErrorResponse> manejarDependenciaNoDisponible(
            DependenciaNoDisponibleException ex, HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "SERVICE_UNAVAILABLE",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    /** Se dispara cuando falla una anotacion @NotNull, @Positive, etc. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidacion(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        String mensajes = ex.getBindingResult().getFieldErrors().stream()
                .map(campo -> campo.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "BAD_REQUEST",
                mensajes,
                request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(error);
    }

    /** Red de seguridad: cualquier error no previsto. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarErrorGeneral(
            Exception ex, HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                "Error interno del servidor: " + ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.internalServerError().body(error);
    }
}