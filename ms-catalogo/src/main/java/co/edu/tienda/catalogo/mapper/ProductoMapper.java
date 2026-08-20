package co.edu.tienda.catalogo.mapper;

import co.edu.tienda.catalogo.dto.ProductoResponse;
import co.edu.tienda.catalogo.entity.Producto;

import java.util.List;

public final class ProductoMapper {

    private ProductoMapper() {
    }

    public static ProductoResponse aResponse(Producto producto) {
        return new ProductoResponse(
                producto.getCodigoProducto(),
                producto.getNombreProducto(),
                producto.getProveedor().getNitproveedor(),
                producto.getProveedor().getNombreProveedor(),
                producto.getPrecioCompra(),
                producto.getIvacompra(),
                producto.getPrecioVenta()
        );
    }

    public static List<ProductoResponse> aResponse(List<Producto> productos) {
        return productos.stream().map(ProductoMapper::aResponse).toList();
    }
}
