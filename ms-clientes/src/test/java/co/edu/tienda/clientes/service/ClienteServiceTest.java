package co.edu.tienda.clientes.service;

import co.edu.tienda.clientes.dto.ClienteRequest;
import co.edu.tienda.clientes.dto.ClienteUpdateRequest;
import co.edu.tienda.clientes.entity.Cliente;
import co.edu.tienda.clientes.exception.RecursoNoEncontradoException;
import co.edu.tienda.clientes.exception.ReglaNegocioException;
import co.edu.tienda.clientes.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClienteServiceTest {

    private ClienteRepository repositorio;
    private ClienteService clienteService;

    @BeforeEach
    void preparar() {
        repositorio = mock(ClienteRepository.class);
        clienteService = new ClienteService(repositorio);
        when(repositorio.save(any(Cliente.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private ClienteRequest peticionNueva() {
        return new ClienteRequest(52147896L, "Carolina Restrepo",
                "Calle 100 # 15-30", "3105558877", "carolina@correo.com");
    }

    private Cliente clienteDemo() {
        return new Cliente(52147896L, "Carolina Restrepo",
                "Calle 100 # 15-30", "3105558877", "carolina@correo.com");
    }

    @Test
    @DisplayName("Crear un cliente nuevo lo guarda con todos sus datos")
    void crearCorrecto() {
        when(repositorio.existsById(52147896L)).thenReturn(false);

        Cliente creado = clienteService.crear(peticionNueva());

        assertEquals(52147896L, creado.getCedulaCliente());
        assertEquals("Carolina Restrepo", creado.getNombreCliente());
        verify(repositorio).save(any(Cliente.class));
    }

    @Test
    @DisplayName("Cédula duplicada lanza ReglaNegocioException y no guarda")
    void cedulaDuplicada() {
        when(repositorio.existsById(52147896L)).thenReturn(true);

        assertThrows(ReglaNegocioException.class,
                () -> clienteService.crear(peticionNueva()));
        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("Consultar una cédula inexistente lanza 404")
    void consultaInexistente() {
        when(repositorio.findById(99999999L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> clienteService.buscarPorCedula(99999999L));
    }

    @Test
    @DisplayName("Actualizar modifica los cuatro campos editables")
    void actualizarCorrecto() {
        when(repositorio.findById(52147896L)).thenReturn(Optional.of(clienteDemo()));

        Cliente actualizado = clienteService.actualizar(52147896L,
                new ClienteUpdateRequest("Carolina R. Pérez", "Carrera 7 # 20-10",
                        "3201112233", "cperez@correo.com"));

        assertEquals("Carolina R. Pérez", actualizado.getNombreCliente());
        assertEquals("Carrera 7 # 20-10", actualizado.getDireccionCliente());
        assertEquals("3201112233", actualizado.getTelefonoCliente());
        assertEquals("cperez@correo.com", actualizado.getEmailCliente());
        // La cédula NO se modifica: es la clave primaria
        assertEquals(52147896L, actualizado.getCedulaCliente());
    }

    @Test
    @DisplayName("Eliminar un cliente inexistente lanza 404 y no borra")
    void eliminarInexistente() {
        when(repositorio.findById(99999999L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> clienteService.eliminar(99999999L));
        verify(repositorio, never()).delete(any());
    }

    @Test
    @DisplayName("La consulta por lote con lista vacía no golpea la base de datos")
    void loteVacio() {
        assertTrue(clienteService.buscarPorCedulas(List.of()).isEmpty());
        assertTrue(clienteService.buscarPorCedulas(null).isEmpty());
        verify(repositorio, never()).findByCedulaClienteIn(any());
    }
}