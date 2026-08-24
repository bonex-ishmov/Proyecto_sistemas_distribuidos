package co.edu.tienda.clientes.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Sin cédula: es la clave primaria y viaja en la URL. */
public record ClienteUpdateRequest(

        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 100)
        String nombreCliente,

        @NotBlank(message = "La dirección es obligatoria")
        @Size(max = 150)
        String direccionCliente,

        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "^[0-9+\\-\\s()]{7,20}$",
                message = "El teléfono solo admite números y los signos + - ( )")
        String telefonoCliente,

        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El correo electrónico no tiene un formato válido")
        @Size(max = 100)
        String emailCliente
) {}