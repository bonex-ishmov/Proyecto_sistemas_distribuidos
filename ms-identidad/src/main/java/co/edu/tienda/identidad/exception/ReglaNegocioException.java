package co.edu.tienda.identidad.exception;

/**
 * Se traduce a HTTP 409 (Conflict). Cubre duplicados y reglas del negocio
 * que impiden completar la operación (por ejemplo, borrar admininicial).
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}