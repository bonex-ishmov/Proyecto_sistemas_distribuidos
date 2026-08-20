package co.edu.tienda.identidad.service;

import co.edu.tienda.identidad.dto.LoginRequest;
import co.edu.tienda.identidad.dto.LoginResponse;
import co.edu.tienda.identidad.entity.Usuario;
import co.edu.tienda.identidad.exception.CredencialesInvalidasException;
import co.edu.tienda.identidad.repository.UsuarioRepository;
import co.edu.tienda.identidad.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UsuarioRepository repositorio;
    private AuthService authService;
    private PasswordEncoder encoder;

    @BeforeEach
    void preparar() {
        repositorio = mock(UsuarioRepository.class);
        encoder = new BCryptPasswordEncoder();
        JwtService jwtService = new JwtService(
                "ClaveDePruebaMuyLargaParaFirmarTokensJWTEnLasPruebas2026", 3600000L);
        authService = new AuthService(repositorio, encoder, jwtService);
    }

    private Usuario usuarioDemo(boolean activo) {
        return new Usuario(1L, "Administrador Inicial",
                "admininicial@tiendagenerica.com", "admininicial",
                encoder.encode("admin123456"), activo);
    }

    @Test
    @DisplayName("Login exitoso devuelve un token")
    void loginExitoso() {
        when(repositorio.findByUsuario("admininicial"))
                .thenReturn(Optional.of(usuarioDemo(true)));

        LoginResponse respuesta = authService.login(
                new LoginRequest("admininicial", "admin123456"));

        assertNotNull(respuesta.token());
        assertEquals("admininicial", respuesta.usuario());
        assertEquals(1L, respuesta.cedulaUsuario());
    }

    @Test
    @DisplayName("Contraseña incorrecta lanza excepción")
    void passwordIncorrecta() {
        when(repositorio.findByUsuario("admininicial"))
                .thenReturn(Optional.of(usuarioDemo(true)));

        assertThrows(CredencialesInvalidasException.class,
                () -> authService.login(new LoginRequest("admininicial", "mala")));
    }

    @Test
    @DisplayName("Usuario inexistente lanza excepción")
    void usuarioInexistente() {
        when(repositorio.findByUsuario(anyString())).thenReturn(Optional.empty());

        assertThrows(CredencialesInvalidasException.class,
                () -> authService.login(new LoginRequest("fantasma", "x")));
    }

    @Test
    @DisplayName("Usuario inactivo no puede ingresar")
    void usuarioInactivo() {
        when(repositorio.findByUsuario("admininicial"))
                .thenReturn(Optional.of(usuarioDemo(false)));

        assertThrows(CredencialesInvalidasException.class,
                () -> authService.login(new LoginRequest("admininicial", "admin123456")));
    }
}