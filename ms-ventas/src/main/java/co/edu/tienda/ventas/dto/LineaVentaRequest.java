package co.edu.tienda.ventas.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Una linea del formulario de venta: el usuario agrega un producto por codigo
 * e indica la cantidad. El precio, el % de IVA y el nombre NO viajan en el
 * body: los resuelve ms-ventas contra ms-catalogo.
 */
public record LineaVentaRequest(

        @NotNull(message = "El codigo de producto es obligatorio")
        @Positive(message = "El codigo de producto debe ser un numero positivo")
        Long codigoProducto,

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor a cero")
        Integer cantidad
) {}
