package co.edu.tienda.reportes.dto;

/**
 * Un cliente en el listado del módulo 7.
 *
 * Campos idénticos a los de ClienteResponse de ms-clientes: este record sirve a
 * la vez para deserializar la respuesta de GET /api/clientes y como cuerpo de la
 * respuesta de ms-reportes.
 */
public record ClienteResponse(
        Long cedulaCliente,
        String nombreCliente,
        String direccionCliente,
        String telefonoCliente,
        String emailCliente
) {}
