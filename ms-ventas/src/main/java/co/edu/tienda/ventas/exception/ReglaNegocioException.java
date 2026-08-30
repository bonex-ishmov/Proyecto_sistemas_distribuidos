package co.edu.tienda.ventas.exception;

/** Se traduce a HTTP 409 (Conflict): duplicados y reglas del negocio. */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}