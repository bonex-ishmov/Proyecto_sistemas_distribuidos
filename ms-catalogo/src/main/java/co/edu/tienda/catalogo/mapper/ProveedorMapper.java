package co.edu.tienda.catalogo.mapper;

import co.edu.tienda.catalogo.dto.ProveedorResponse;
import co.edu.tienda.catalogo.entity.Proveedor;

import java.util.List;

public final class ProveedorMapper {

    private ProveedorMapper() {
    }

    public static ProveedorResponse aResponse(Proveedor proveedor) {
        return new ProveedorResponse(
                proveedor.getNitproveedor(),
                proveedor.getNombreProveedor(),
                proveedor.getDireccionProveedor(),
                proveedor.getTelefonoProveedor(),
                proveedor.getCiudadProveedor()
        );
    }

    public static List<ProveedorResponse> aResponse(List<Proveedor> proveedores) {
        return proveedores.stream().map(ProveedorMapper::aResponse).toList();
    }
}
