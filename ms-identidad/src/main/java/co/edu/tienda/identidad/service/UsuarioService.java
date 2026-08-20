package co.edu.tienda.identidad.service;

import co.edu.tienda.identidad.dto.UsuarioRequest;
import co.edu.tienda.identidad.dto.UsuarioUpdateRequest;
import co.edu.tienda.identidad.entity.Usuario;
import co.edu.tienda.identidad.exception.RecursoNoEncontradoException;
import co.edu.tienda.identidad.exception.ReglaNegocioException;
import co.edu.tienda.identidad.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String nombreUsuarioInicial;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${app.usuario-inicial.usuario}") String nombreUsuarioInicial) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.nombreUsuarioInicial = nombreUsuarioInicial;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorCedula(Long cedula) {
        return usuarioRepository.findById(cedula)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario Inexistente"));
    }

    @Transactional
    public Usuario crear(UsuarioRequest request) {

        if (usuarioRepository.existsById(request.cedulaUsuario())) {
            throw new ReglaNegocioException(
                    "Ya existe un usuario con la cédula " + request.cedulaUsuario());
        }
        if (usuarioRepository.existsByUsuario(request.usuario())) {
            throw new ReglaNegocioException(
                    "El nombre de usuario '" + request.usuario() + "' ya está en uso");
        }
        if (usuarioRepository.existsByEmailUsuario(request.emailUsuario())) {
            throw new ReglaNegocioException(
                    "El correo '" + request.emailUsuario() + "' ya está registrado");
        }

        Usuario nuevo = new Usuario(
                request.cedulaUsuario(),
                request.nombreUsuario(),
                request.emailUsuario(),
                request.usuario(),
                passwordEncoder.encode(request.password()),
                true
        );
        Usuario guardado = usuarioRepository.save(nuevo);

        desactivarUsuarioInicialSiCorresponde(guardado);

        return guardado;
    }

    @Transactional
    public Usuario actualizar(Long cedula, UsuarioUpdateRequest request) {

        Usuario usuario = buscarPorCedula(cedula);

        // Solo validamos duplicados si el dato realmente cambió
        if (!usuario.getUsuario().equals(request.usuario())
                && usuarioRepository.existsByUsuario(request.usuario())) {
            throw new ReglaNegocioException(
                    "El nombre de usuario '" + request.usuario() + "' ya está en uso");
        }
        if (!usuario.getEmailUsuario().equals(request.emailUsuario())
                && usuarioRepository.existsByEmailUsuario(request.emailUsuario())) {
            throw new ReglaNegocioException(
                    "El correo '" + request.emailUsuario() + "' ya está registrado");
        }

        usuario.setNombreUsuario(request.nombreUsuario());
        usuario.setEmailUsuario(request.emailUsuario());
        usuario.setUsuario(request.usuario());

        if (request.password() != null && !request.password().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(request.password()));
        }

        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void eliminar(Long cedula) {
        Usuario usuario = buscarPorCedula(cedula);

        if (nombreUsuarioInicial.equals(usuario.getUsuario())) {
            throw new ReglaNegocioException(
                    "El usuario inicial no se puede eliminar, solo desactivar");
        }

        usuarioRepository.delete(usuario);
    }

    @Transactional
    public Usuario cambiarEstado(Long cedula, boolean activo) {
        Usuario usuario = buscarPorCedula(cedula);
        usuario.setActivo(activo);
        return usuarioRepository.save(usuario);
    }

    /**
     * Requisito RF-02 del documento: "En este módulo se desactivará el usuario
     * adminicial". Regla acordada (A-4): se desactiva automáticamente en cuanto
     * se crea el primer usuario real, para que el sistema nunca quede sin acceso.
     */
    private void desactivarUsuarioInicialSiCorresponde(Usuario recienCreado) {

        if (nombreUsuarioInicial.equals(recienCreado.getUsuario())) {
            return; // el creado ES el inicial: no aplica
        }

        usuarioRepository.findByUsuario(nombreUsuarioInicial)
                .filter(Usuario::getActivo)
                .ifPresent(inicial -> {
                    inicial.setActivo(false);
                    usuarioRepository.save(inicial);
                    log.info("Usuario inicial '{}' desactivado tras crear el usuario '{}'.",
                            nombreUsuarioInicial, recienCreado.getUsuario());
                });
    }
}