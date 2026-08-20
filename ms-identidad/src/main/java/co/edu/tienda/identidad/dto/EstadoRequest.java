package co.edu.tienda.identidad.dto;

import jakarta.validation.constraints.NotNull;

/** Para activar o desactivar un usuario manualmente (decisión A-4). */
public record EstadoRequest(

        @NotNull(message = "El estado 'activo' es obligatorio")
        Boolean activo
) {}