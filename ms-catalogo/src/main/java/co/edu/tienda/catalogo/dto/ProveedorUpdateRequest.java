package co.edu.tienda.catalogo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Sin NIT: es la clave primaria y viaja en la URL. */
public record ProveedorUpdateRequest(

        @NotBlank(message = "El nombre del proveedor es obligatorio")
        @Size(max = 100)
        String nombreProveedor,

        @NotBlank(message = "La direccion es obligatoria")
        @Size(max = 150)
        String direccionProveedor,

        @NotBlank(message = "El telefono es obligatorio")
        @Pattern(regexp = "^[0-9+\\-\\s()]{7,20}$",
                 message = "El telefono solo admite numeros y los signos + - ( )")
        String telefonoProveedor,

        @NotBlank(message = "La ciudad es obligatoria")
        @Size(max = 60)
        String ciudadProveedor
) {}
