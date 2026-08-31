package co.edu.tienda.identidad.config;

import co.edu.tienda.identidad.entity.Usuario;
import co.edu.tienda.identidad.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Siembra el usuario de sistema con el que ms-ventas y ms-reportes se
 * autentican contra ms-identidad para llamar a los endpoints /lote
 * (decisión de CLAUDE.md: autenticación servicio-a-servicio sin API Gateway).
 *
 * Es UN SOLO usuario compartido (cédula 2): ms-reportes solo hace lecturas y
 * reutiliza las credenciales de ms-ventas, no tiene identidad propia.
 *
 * Igual que {@link DataInitializer}: se ejecuta al arrancar y es idempotente
 * (si el usuario ya existe, no hace nada).
 *
 * IMPORTANTE: la inserción es DIRECTA con el repositorio, NO pasa por
 * UsuarioService.crear. Así no se dispara la regla A-4, que desactiva a
 * admininicial en cuanto se crea el primer usuario por la vía normal.
 *
 * Este usuario (cédula 2) debe excluirse del listado de usuarios de ms-reportes.
 */
@Component
public class UsuarioSistemaInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UsuarioSistemaInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.usuario-sistema.cedula}")   private Long cedula;
    @Value("${app.usuario-sistema.nombre}")   private String nombre;
    @Value("${app.usuario-sistema.email}")    private String email;
    @Value("${app.usuario-sistema.usuario}")  private String usuario;
    @Value("${app.usuario-sistema.password}") private String password;

    public UsuarioSistemaInitializer(UsuarioRepository usuarioRepository,
                                     PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.existsByUsuario(usuario)) {
            log.info("El usuario de sistema '{}' ya existe. No se crea de nuevo.", usuario);
            return;
        }

        Usuario sistema = new Usuario(
                cedula,
                nombre,
                email,
                usuario,
                passwordEncoder.encode(password),  // se guarda el HASH, nunca el texto plano
                true
        );
        usuarioRepository.save(sistema);
        log.info("Usuario de sistema '{}' (cédula {}) creado por inserción directa.",
                usuario, cedula);
    }
}
