package co.edu.tienda.ventas.mapper;

import co.edu.tienda.ventas.dto.LineaVentaResponse;
import co.edu.tienda.ventas.dto.VentaResponse;
import co.edu.tienda.ventas.entity.DetalleVenta;
import co.edu.tienda.ventas.entity.Venta;

import java.util.List;

/** Clase utilitaria estatica, misma convencion que UsuarioMapper / ProductoMapper. */
public final class VentaMapper {

    private VentaMapper() {
    }

    public static VentaResponse aResponse(Venta venta) {
        return new VentaResponse(
                venta.getCodigoVenta(),
                venta.getCedulaCliente(),
                venta.getCedulaUsuario(),
                venta.getTotalVenta(),
                venta.getValorIva(),
                venta.getTotalConIva(),
                venta.getFechaVenta(),
                venta.getDetalles().stream().map(VentaMapper::aResponse).toList()
        );
    }

    private static LineaVentaResponse aResponse(DetalleVenta detalle) {
        return new LineaVentaResponse(
                detalle.getIdDetalle(),
                detalle.getCodigoProducto(),
                detalle.getNombreProducto(),
                detalle.getCantidad(),
                detalle.getValorUnitario(),
                detalle.getIvaPorcentaje(),
                detalle.getValorIva(),
                detalle.getValorTotal()
        );
    }

    public static List<VentaResponse> aResponse(List<Venta> ventas) {
        return ventas.stream().map(VentaMapper::aResponse).toList();
    }
}
