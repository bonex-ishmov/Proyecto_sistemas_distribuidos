package co.edu.tienda.clientes.mapper;

import co.edu.tienda.clientes.dto.ClienteResponse;
import co.edu.tienda.clientes.entity.Cliente;

import java.util.List;

public final class ClienteMapper {

    private ClienteMapper() {
    }

    public static ClienteResponse aResponse(Cliente cliente) {
        return new ClienteResponse(
                cliente.getCedulaCliente(),
                cliente.getNombreCliente(),
                cliente.getDireccionCliente(),
                cliente.getTelefonoCliente(),
                cliente.getEmailCliente()
        );
    }

    public static List<ClienteResponse> aResponse(List<Cliente> clientes) {
        return clientes.stream().map(ClienteMapper::aResponse).toList();
    }
}