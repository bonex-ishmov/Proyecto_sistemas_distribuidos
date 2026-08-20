package co.edu.tienda.identidad.exception;

/** Se traduce a HTTP 404. Reutilizable en todos los microservicios. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}