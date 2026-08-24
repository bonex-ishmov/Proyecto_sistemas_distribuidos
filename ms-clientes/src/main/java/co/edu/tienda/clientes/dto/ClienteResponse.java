package co.edu.tienda.clientes.dto;

public record ClienteResponse(
        Long cedulaCliente,
        String nombreCliente,
        String direccionCliente,
        String telefonoCliente,
        String emailCliente
) {}