package co.edu.tienda.ventas.service;

import co.edu.tienda.ventas.dto.LineaVentaRequest;
import co.edu.tienda.ventas.dto.RegistrarVentaRequest;
import co.edu.tienda.ventas.entity.DetalleVenta;
import co.edu.tienda.ventas.entity.Venta;
import co.edu.tienda.ventas.exception.RecursoNoEncontradoException;
import co.edu.tienda.ventas.exception.ReglaNegocioException;
import co.edu.tienda.ventas.gateway.CatalogoGateway;
import co.edu.tienda.ventas.gateway.ClientesGateway;
import co.edu.tienda.ventas.gateway.ProductoInfo;
import co.edu.tienda.ventas.repository.VentaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class VentaService {

    private static final Logger log = LoggerFactory.getLogger(VentaService.class);

    /** Todos los importes de dinero se guardan con 2 decimales. */
    private static final int ESCALA = 2;
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final VentaRepository ventaRepository;
    private final CatalogoGateway catalogoGateway;
    private final ClientesGateway clientesGateway;

    public VentaService(VentaRepository ventaRepository,
                        CatalogoGateway catalogoGateway,
                        ClientesGateway clientesGateway) {
        this.ventaRepository = ventaRepository;
        this.catalogoGateway = catalogoGateway;
        this.clientesGateway = clientesGateway;
    }

    /**
     * Registra una venta completa (cabecera + detalle) en una sola transaccion.
     *
     * @param request       cliente y lineas (codigo de producto + cantidad)
     * @param cedulaUsuario  cedula del vendedor, tomada del claim "cedula" del JWT
     *                       y usada tal cual: el token ya prueba que ms-identidad
     *                       autentico a ese usuario, no se revalida por HTTP.
     */
    @Transactional
    public Venta registrarVenta(RegistrarVentaRequest request, Long cedulaUsuario) {

        if (cedulaUsuario == null) {
            throw new ReglaNegocioException("No se pudo determinar el usuario de la venta");
        }
        if (request.detalles() == null || request.detalles().isEmpty()) {
            throw new ReglaNegocioException("La venta debe tener al menos un producto");
        }
        for (LineaVentaRequest linea : request.detalles()) {
            if (linea.cantidad() == null || linea.cantidad() <= 0) {
                throw new ReglaNegocioException(
                        "La cantidad de cada producto debe ser mayor a cero");
            }
        }

        if (!clientesGateway.existeCliente(request.cedulaCliente())) {
            throw new RecursoNoEncontradoException("Cliente Inexistente");
        }

        Map<Long, ProductoInfo> productos = resolverProductos(request.detalles());

        Venta venta = new Venta();
        venta.setCedulaCliente(request.cedulaCliente());
        venta.setCedulaUsuario(cedulaUsuario);
        venta.setFechaVenta(LocalDateTime.now());

        BigDecimal totalVenta = BigDecimal.ZERO;
        BigDecimal totalIva = BigDecimal.ZERO;

        for (LineaVentaRequest linea : request.detalles()) {
            ProductoInfo producto = productos.get(linea.codigoProducto());

            BigDecimal cantidad = BigDecimal.valueOf(linea.cantidad());
            BigDecimal valorUnitario = escala(producto.precioVenta());
            BigDecimal ivaPorcentaje = producto.ivaPorcentaje();

            // Base SIN IVA de la linea.
            BigDecimal valorTotalLinea = escala(valorUnitario.multiply(cantidad));
            // IVA de la linea: base * (% / 100).
            BigDecimal valorIvaLinea = valorTotalLinea
                    .multiply(ivaPorcentaje)
                    .divide(CIEN, ESCALA, RoundingMode.HALF_UP);

            DetalleVenta detalle = new DetalleVenta();
            detalle.setCodigoProducto(producto.codigo());
            detalle.setNombreProducto(producto.nombre());
            detalle.setCantidad(linea.cantidad());
            detalle.setValorUnitario(valorUnitario);
            detalle.setIvaPorcentaje(ivaPorcentaje);
            detalle.setValorIva(valorIvaLinea);
            detalle.setValorTotal(valorTotalLinea);
            venta.agregarDetalle(detalle);

            totalVenta = totalVenta.add(valorTotalLinea);
            totalIva = totalIva.add(valorIvaLinea);
        }

        venta.setTotalVenta(escala(totalVenta));
        venta.setValorIva(escala(totalIva));
        venta.setTotalConIva(escala(totalVenta.add(totalIva)));

        Venta guardada = ventaRepository.save(venta);
        log.info("Venta {} registrada: cliente {}, usuario {}, {} lineas, total con IVA {}",
                guardada.getCodigoVenta(), guardada.getCedulaCliente(),
                guardada.getCedulaUsuario(), guardada.getDetalles().size(),
                guardada.getTotalConIva());
        return guardada;
    }

    /**
     * Pide a ms-catalogo los productos de la venta en UNA sola llamada y verifica
     * que todos existan. Si falta alguno, la venta entera falla (404).
     */
    private Map<Long, ProductoInfo> resolverProductos(List<LineaVentaRequest> detalles) {

        List<Long> codigos = detalles.stream()
                .map(LineaVentaRequest::codigoProducto)
                .distinct()
                .toList();

        Map<Long, ProductoInfo> encontrados = catalogoGateway.buscarProductos(codigos).stream()
                .collect(Collectors.toMap(ProductoInfo::codigo, Function.identity(),
                        (a, b) -> a));

        LinkedHashSet<Long> faltantes = new LinkedHashSet<>();
        for (Long codigo : codigos) {
            if (!encontrados.containsKey(codigo)) {
                faltantes.add(codigo);
            }
        }
        if (!faltantes.isEmpty()) {
            throw new RecursoNoEncontradoException("Producto Inexistente: " + faltantes);
        }
        return encontrados;
    }

    private static BigDecimal escala(BigDecimal valor) {
        return valor.setScale(ESCALA, RoundingMode.HALF_UP);
    }
}
