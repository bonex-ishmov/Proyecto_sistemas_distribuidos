package co.edu.tienda.clientes.service;

import co.edu.tienda.clientes.dto.ClienteRequest;
import co.edu.tienda.clientes.dto.ClienteUpdateRequest;
import co.edu.tienda.clientes.entity.Cliente;
import co.edu.tienda.clientes.exception.RecursoNoEncontradoException;
import co.edu.tienda.clientes.exception.ReglaNegocioException;
import co.edu.tienda.clientes.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional(readOnly = true)
    public List<Cliente> listar() {
        return clienteRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Cliente buscarPorCedula(Long cedula) {
        return clienteRepository.findById(cedula)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente Inexistente"));
    }

    /** Consulta por lote. Devuelve solo los que existen, sin lanzar error. */
    @Transactional(readOnly = true)
    public List<Cliente> buscarPorCedulas(List<Long> cedulas) {
        if (cedulas == null || cedulas.isEmpty()) {
            return List.of();
        }
        return clienteRepository.findByCedulaClienteIn(cedulas);
    }

    @Transactional
    public Cliente crear(ClienteRequest request) {

        if (clienteRepository.existsById(request.cedulaCliente())) {
            throw new ReglaNegocioException(
                    "Ya existe un cliente con la cédula " + request.cedulaCliente());
        }

        Cliente nuevo = new Cliente(
                request.cedulaCliente(),
                request.nombreCliente(),
                request.direccionCliente(),
                request.telefonoCliente(),
                request.emailCliente()
        );
        return clienteRepository.save(nuevo);
    }

    @Transactional
    public Cliente actualizar(Long cedula, ClienteUpdateRequest request) {

        Cliente cliente = buscarPorCedula(cedula);

        cliente.setNombreCliente(request.nombreCliente());
        cliente.setDireccionCliente(request.direccionCliente());
        cliente.setTelefonoCliente(request.telefonoCliente());
        cliente.setEmailCliente(request.emailCliente());

        return clienteRepository.save(cliente);
    }

    @Transactional
    public void eliminar(Long cedula) {
        Cliente cliente = buscarPorCedula(cedula);
        clienteRepository.delete(cliente);
    }
}