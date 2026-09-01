package co.edu.tienda.reportes.dto;

/**
 * Un usuario en el listado del módulo 7.
 *
 * Campos idénticos a los de UsuarioResponse de ms-identidad: este record sirve
 * a la vez para deserializar la respuesta de GET /api/usuarios y como cuerpo de
 * la respuesta de ms-reportes. Nunca incluye password ni hash (decisión A-5).
 */
public record UsuarioResponse(
        Long cedulaUsuario,
        String nombreUsuario,
        String emailUsuario,
        String usuario,
        Boolean activo
) {}
