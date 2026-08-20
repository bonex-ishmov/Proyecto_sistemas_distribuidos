package co.edu.tienda.identidad.service;

import co.edu.tienda.identidad.dto.UsuarioRequest;
import co.edu.tienda.identidad.dto.UsuarioUpdateRequest;
import co.edu.tienda.identidad.entity.Usuario;
import co.edu.tienda.identidad.exception.RecursoNoEncontradoException;
import co.edu.tienda.identidad.exception.ReglaNegocioException;
import co.edu.tienda.identidad.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UsuarioServiceTest {

    private UsuarioRepository repositorio;
    private UsuarioService usuarioService;
    private PasswordEncoder encoder;

    @BeforeEach
    void preparar() {
        repositorio = mock(UsuarioRepository.class);
        encoder = new BCryptPasswordEncoder();
        usuarioService = new UsuarioService(repositorio, encoder, "admininicial");
        // save() devuelve el mismo objeto que recibe
        when(repositorio.save(any(Usuario.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private UsuarioRequest peticionNueva() {
        return new UsuarioRequest(1002L, "Laura Gómez",
                "laura@tienda.com", "lgomez", "laura123456");
    }

    @Test
    @DisplayName("Al crear un usuario, la contraseña se guarda hasheada")
    void passwordSeHashea() {
        when(repositorio.existsById(1002L)).thenReturn(false);
        when(repositorio.findByUsuario("admininicial")).thenReturn(Optional.empty());

        Usuario creado = usuarioService.crear(peticionNueva());

        assertNotEquals("laura123456", creado.getPassword());
        assertTrue(encoder.matches("laura123456", creado.getPassword()));
    }

    @Test
    @DisplayName("Cédula duplicada lanza ReglaNegocioException")
    void cedulaDuplicada() {
        when(repositorio.existsById(1002L)).thenReturn(true);

        assertThrows(ReglaNegocioException.class,
                () -> usuarioService.crear(peticionNueva()));
    }

    @Test
    @DisplayName("Nombre de usuario duplicado lanza ReglaNegocioException")
    void usuarioDuplicado() {
        when(repositorio.existsById(1002L)).thenReturn(false);
        when(repositorio.existsByUsuario("lgomez")).thenReturn(true);

        assertThrows(ReglaNegocioException.class,
                () -> usuarioService.crear(peticionNueva()));
    }

    @Test
    @DisplayName("A-4: crear el primer usuario real desactiva a admininicial")
    void desactivaUsuarioInicial() {
        Usuario inicial = new Usuario(1L, "Administrador Inicial",
                "admin@tienda.com", "admininicial", encoder.encode("admin123456"), true);

        when(repositorio.existsById(1002L)).thenReturn(false);
        when(repositorio.findByUsuario("admininicial")).thenReturn(Optional.of(inicial));

        usuarioService.crear(peticionNueva());

        assertFalse(inicial.getActivo());
    }

    @Test
    @DisplayName("Consultar una cédula inexistente lanza 404")
    void consultaInexistente() {
        when(repositorio.findById(99999L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> usuarioService.buscarPorCedula(99999L));
    }

    @Test
    @DisplayName("Actualizar sin contraseña conserva la anterior")
    void actualizarSinPassword() {
        String hashOriginal = encoder.encode("original123");
        Usuario existente = new Usuario(1002L, "Laura", "laura@tienda.com",
                "lgomez", hashOriginal, true);
        when(repositorio.findById(1002L)).thenReturn(Optional.of(existente));

        Usuario actualizado = usuarioService.actualizar(1002L,
                new UsuarioUpdateRequest("Laura G.", "laura@tienda.com", "lgomez", null));

        assertEquals(hashOriginal, actualizado.getPassword());
        assertEquals("Laura G.", actualizado.getNombreUsuario());
    }

    @Test
    @DisplayName("No se puede eliminar el usuario inicial")
    void noEliminaUsuarioInicial() {
        Usuario inicial = new Usuario(1L, "Administrador Inicial",
                "admin@tienda.com", "admininicial", "hash", false);
        when(repositorio.findById(1L)).thenReturn(Optional.of(inicial));

        assertThrows(ReglaNegocioException.class, () -> usuarioService.eliminar(1L));
        verify(repositorio, never()).delete(any());
    }
}