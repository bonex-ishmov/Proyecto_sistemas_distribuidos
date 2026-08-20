package co.edu.tienda.identidad.dto;

/**
 * Respuesta del login. Observa que NO devuelve la contraseña ni el hash.
 */
public record LoginResponse(
        String token,
        String tipo,
        Long cedulaUsuario,
        String usuario,
        String nombreUsuario,
        long expiraEnMs
) {}