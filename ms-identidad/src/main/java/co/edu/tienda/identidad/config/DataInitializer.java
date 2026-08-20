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
 * Siembra el usuario inicial exigido por la Parte 1 del documento.
 * Se ejecuta al arrancar la aplicación y es idempotente:
 * si el usuario ya existe, no hace nada.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.usuario-inicial.cedula}")   private Long cedula;
    @Value("${app.usuario-inicial.nombre}")   private String nombre;
    @Value("${app.usuario-inicial.email}")    private String email;
    @Value("${app.usuario-inicial.usuario}")  private String usuario;
    @Value("${app.usuario-inicial.password}") private String password;

    public DataInitializer(UsuarioRepository usuarioRepository,
                           PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.existsByUsuario(usuario)) {
            log.info("El usuario inicial '{}' ya existe. No se crea de nuevo.", usuario);
            return;
        }

        Usuario inicial = new Usuario(
                cedula,
                nombre,
                email,
                usuario,
                passwordEncoder.encode(password),  // <-- se guarda el HASH
                true
        );
        usuarioRepository.save(inicial);
        log.info("Usuario inicial '{}' creado correctamente.", usuario);
    }
}