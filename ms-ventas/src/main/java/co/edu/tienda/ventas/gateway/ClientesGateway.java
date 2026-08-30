package co.edu.tienda.ventas.gateway;

/**
 * Puerto hacia ms-clientes. Solo contrato en el Incremento 2.
 * Implementacion real (HTTP contra ms-clientes) en el Incremento 4.
 */
public interface ClientesGateway {

    /** true si existe un cliente con esa cedula en ms-clientes. */
    boolean existeCliente(Long cedulaCliente);
}
