package co.edu.tienda.identidad.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Datos que llegan del frontend al hacer login.
 * Es un record: inmutable y sin getters/setters manuales.
 */
public record LoginRequest(

        @NotBlank(message = "El usuario es obligatorio")
        String usuario,

        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {}