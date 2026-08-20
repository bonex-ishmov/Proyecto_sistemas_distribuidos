package co.edu.tienda.catalogo.exception;

import java.util.List;

/**
 * El archivo esta bien formado, pero su CONTENIDO tiene errores:
 * tipos de datos, proveedores inexistentes, codigos duplicados...
 * Lleva la lista de errores con el numero de linea de cada uno.
 * Se traduce a HTTP 422 (Unprocessable Entity).
 */
public class DatosInvalidosException extends RuntimeException {

    private final List<String> errores;

    public DatosInvalidosException(String mensaje, List<String> errores) {
        super(mensaje);
        this.errores = errores;
    }

    public List<String> getErrores() {
        return errores;
    }
}
