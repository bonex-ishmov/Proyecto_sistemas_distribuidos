package co.edu.tienda.catalogo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * OJO: el NIT solo se valida como numero positivo, SIN longitud minima.
 * El CSV de muestra del documento usa NITs 1, 2, 3, 4 y 5.
 */
public record ProveedorRequest(

        @NotNull(message = "El NIT es obligatorio")
        @Positive(message = "El NIT debe ser un numero positivo")
        Long nitproveedor,

        @NotBlank(message = "El nombre del proveedor es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
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
