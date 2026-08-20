package co.edu.tienda.catalogo.exception;

/**
 * Problemas con el ARCHIVO como tal: no se envio, esta vacio,
 * no es CSV o su estructura de columnas no corresponde.
 * Se traduce a HTTP 400.
 */
public class ArchivoInvalidoException extends RuntimeException {

    public ArchivoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
