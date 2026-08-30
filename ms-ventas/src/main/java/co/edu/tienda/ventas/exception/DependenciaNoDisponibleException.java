package co.edu.tienda.ventas.exception;

/**
 * Se traduce a HTTP 503 (Service Unavailable). La lanzaran los clientes HTTP
 * hacia ms-clientes, ms-catalogo y ms-identidad cuando el servicio remoto no
 * responde, devuelve 5xx o hay timeout.
 *
 * Decision acordada: si una dependencia falla al registrar una venta, la venta
 * falla completa y no se persiste nada. Se crea aqui, en el esqueleto, para que
 * el GlobalExceptionHandler quede completo; empieza a usarse en el Incremento 4.
 */
public class DependenciaNoDisponibleException extends RuntimeException {

    public DependenciaNoDisponibleException(String mensaje) {
        super(mensaje);
    }

    public DependenciaNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}