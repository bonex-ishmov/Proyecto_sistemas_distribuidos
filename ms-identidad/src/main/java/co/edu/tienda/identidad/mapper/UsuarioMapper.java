package co.edu.tienda.identidad.mapper;

import co.edu.tienda.identidad.dto.UsuarioResponse;
import co.edu.tienda.identidad.entity.Usuario;

import java.util.List;

/**
 * Convierte la entidad (lo que vive en la BD) al DTO (lo que sale por la API).
 * Esta separación es la que garantiza que el hash de la contraseña
 * nunca pueda escaparse por accidente en una respuesta.
 */
public final class UsuarioMapper {

    private UsuarioMapper() {
        // Clase de utilidad: no se instancia
    }

    public static UsuarioResponse aResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getCedulaUsuario(),
                usuario.getNombreUsuario(),
                usuario.getEmailUsuario(),
                usuario.getUsuario(),
                usuario.getActivo()
        );
    }

    public static List<UsuarioResponse> aResponse(List<Usuario> usuarios) {
        return usuarios.stream().map(UsuarioMapper::aResponse).toList();
    }
}