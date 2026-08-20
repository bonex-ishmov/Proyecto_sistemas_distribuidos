package co.edu.tienda.catalogo.dto;

public record ProveedorResponse(
        Long nitproveedor,
        String nombreProveedor,
        String direccionProveedor,
        String telefonoProveedor,
        String ciudadProveedor
) {}
