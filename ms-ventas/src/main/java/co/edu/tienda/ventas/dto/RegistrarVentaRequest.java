package co.edu.tienda.ventas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * Cuerpo de POST /api/ventas.
 *
 * NO incluye la cedula del usuario: sale del JWT (claim "cedula"), nunca del
 * body. Sin limite de lineas en el backend (el "maximo 3 productos" del PDF es
 * solo la maqueta del frontend).
 */
public record RegistrarVentaRequest(

        @NotNull(message = "La cedula del cliente es obligatoria")
        @Positive(message = "La cedula del cliente debe ser un numero positivo")
        Long cedulaCliente,

        @NotEmpty(message = "La venta debe tener al menos un producto")
        @Valid
        List<LineaVentaRequest> detalles
) {}
