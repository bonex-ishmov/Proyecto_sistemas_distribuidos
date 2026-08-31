package co.edu.tienda.reportes.exception;

/**
 * Se traduce a HTTP 503 (Service Unavailable). La lanzaran los adaptadores HTTP
 * hacia ms-identidad, ms-clientes y ms-ventas cuando el servicio remoto no
 * responde, devuelve 5xx o hay timeout.
 *
 * Como ms-reportes es solo de lectura, un 503 significa "no pude construir el
 * reporte porque una fuente no respondio". Se crea aqui, en el esqueleto, para
 * que el GlobalExceptionHandler quede completo; empieza a usarse en el
 * Incremento 2.
 */
public class DependenciaNoDisponibleException extends RuntimeException {

    public DependenciaNoDisponibleException(String mensaje) {
        super(mensaje);
    }

    public DependenciaNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
