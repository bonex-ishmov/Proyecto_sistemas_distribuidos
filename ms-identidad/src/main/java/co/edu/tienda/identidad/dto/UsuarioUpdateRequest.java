package co.edu.tienda.identidad.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para ACTUALIZAR. No lleva cédula: esa viene en la URL y no se cambia
 * (es la clave primaria). La contraseña es OPCIONAL: si llega vacía o nula,
 * se conserva la que ya tenía el usuario.
 */
public record UsuarioUpdateRequest(

        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 100)
        String nombreUsuario,

        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El correo electrónico no tiene un formato válido")
        @Size(max = 100)
        String emailUsuario,

        @NotBlank(message = "El usuario es obligatorio")
        @Size(max = 50)
        String usuario,

        @Size(min = 6, max = 100, message = "La contraseña debe tener al menos 6 caracteres")
        String password
) {}