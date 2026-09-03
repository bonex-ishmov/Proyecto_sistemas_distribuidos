package co.edu.tienda.ventas.service;

import co.edu.tienda.ventas.dto.LineaVentaRequest;
import co.edu.tienda.ventas.dto.RegistrarVentaRequest;
import co.edu.tienda.ventas.dto.ResumenPeriodoResponse;
import co.edu.tienda.ventas.dto.ResumenVentasResponse;
import co.edu.tienda.ventas.dto.TotalPorClienteResponse;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

    /**
     * Zona en la que se define "hoy" y "mes en curso" para GET /api/ventas/resumen.
     *
     * fecha_venta se escribe con LocalDateTime.now() (hora de pared del JVM, que
     * corre en Bogota) y el driver MySQL la guarda VERBATIM en la columna
     * DATETIME: se verifico el round-trip real contra Railway y no hay conversion
     * de zona pese a serverTimezone=UTC en la URL (ese parametro solo afecta a
     * tipos "instante" como Timestamp, no a LocalDateTime). Por eso los limites
     * del periodo se calculan en esta misma zona: asi "hoy" coincide con la fecha
     * local con la que quedaron guardadas las ventas y una venta de las 7pm no se
     * cuenta en el dia siguiente.
     */
    private static final ZoneId ZONA_TIENDA = ZoneId.of("America/Bogota");

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

    /** GET /api/ventas/{codigo}. 404 si el consecutivo no existe. */
    @Transactional(readOnly = true)
    public Venta buscarPorCodigo(Long codigo) {
        return ventaRepository.findById(codigo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta Inexistente"));
    }

    /**
     * GET /api/ventas/total-por-cliente. Una fila por cliente con ventas,
     * con la suma de total_con_iva. Lista vacia si aun no hay ventas.
     */
    @Transactional(readOnly = true)
    public List<TotalPorClienteResponse> totalPorCliente() {
        return ventaRepository.totalConIvaPorCliente();
    }

    /**
     * GET /api/ventas/resumen. Ventas de hoy y del mes en curso (cantidad y monto
     * total con IVA), para el dashboard de inicio. "Hoy" y "mes" se definen en
     * {@link #ZONA_TIENDA}. Cada periodo es una consulta agregada aparte.
     */
    @Transactional(readOnly = true)
    public ResumenVentasResponse resumen() {
        LocalDate hoy = LocalDate.now(ZONA_TIENDA);
        LocalDateTime inicioHoy = hoy.atStartOfDay();
        LocalDateTime inicioManana = hoy.plusDays(1).atStartOfDay();
        LocalDateTime inicioMes = hoy.withDayOfMonth(1).atStartOfDay();
        LocalDateTime inicioMesSiguiente = inicioMes.plusMonths(1);

        return new ResumenVentasResponse(
                normalizar(ventaRepository.resumenEntre(inicioHoy, inicioManana)),
                normalizar(ventaRepository.resumenEntre(inicioMes, inicioMesSiguiente)));
    }

    /** Deja el monto del periodo siempre con 2 decimales (y nunca null). */
    private static ResumenPeriodoResponse normalizar(ResumenPeriodoResponse periodo) {
        BigDecimal monto = periodo.montoTotalConIva() == null
                ? BigDecimal.ZERO
                : periodo.montoTotalConIva();
        return new ResumenPeriodoResponse(periodo.cantidad(), escala(monto));
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
