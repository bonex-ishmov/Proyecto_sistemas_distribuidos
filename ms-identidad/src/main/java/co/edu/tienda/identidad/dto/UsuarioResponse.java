package co.edu.tienda.identidad.dto;

/**
 * Lo que el sistema DEVUELVE. Decisión A-5: nunca incluye password ni el hash.
 */
public record UsuarioResponse(
        Long cedulaUsuario,
        String nombreUsuario,
        String emailUsuario,
        String usuario,
        Boolean activo
) {}