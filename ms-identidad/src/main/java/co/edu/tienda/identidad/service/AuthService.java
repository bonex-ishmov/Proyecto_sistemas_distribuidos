package co.edu.tienda.identidad.service;

import co.edu.tienda.identidad.dto.LoginRequest;
import co.edu.tienda.identidad.dto.LoginResponse;
import co.edu.tienda.identidad.entity.Usuario;
import co.edu.tienda.identidad.exception.CredencialesInvalidasException;
import co.edu.tienda.identidad.repository.UsuarioRepository;
import co.edu.tienda.identidad.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final String MENSAJE_ERROR =
            "Usuario o contraseña errados, intente de nuevo";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {

        Usuario usuario = usuarioRepository.findByUsuario(request.usuario())
                .orElseThrow(() -> new CredencialesInvalidasException(MENSAJE_ERROR));

        if (Boolean.FALSE.equals(usuario.getActivo())) {
            throw new CredencialesInvalidasException(
                    "El usuario se encuentra inactivo");
        }

        // matches() re-hashea lo que el usuario escribió y lo compara
        // con el hash guardado. NUNCA se descifra el hash: eso es imposible.
        if (!passwordEncoder.matches(request.password(), usuario.getPassword())) {
            throw new CredencialesInvalidasException(MENSAJE_ERROR);
        }

        String token = jwtService.generarToken(usuario);

        return new LoginResponse(
                token,
                "Bearer",
                usuario.getCedulaUsuario(),
                usuario.getUsuario(),
                usuario.getNombreUsuario(),
                jwtService.getExpiracionMs()
        );
    }
}