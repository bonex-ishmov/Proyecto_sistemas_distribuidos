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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Unitario de la capa service, mismo estilo que el resto del monorepo:
 * JUnit 5 + Mockito, sin contexto Spring. Los gateways (ms-catalogo, ms-clientes)
 * van mockeados; el HTTP real llega en el Incremento 4. La cedula del usuario
 * sale del JWT y se usa tal cual, no se revalida contra ms-identidad.
 */
class VentaServiceTest {

    private static final Long CEDULA_USUARIO = 111L;
    private static final Long CEDULA_CLIENTE = 222L;

    private VentaRepository ventaRepository;
    private CatalogoGateway catalogoGateway;
    private ClientesGateway clientesGateway;
    private VentaService ventaService;

    @BeforeEach
    void preparar() {
        ventaRepository = mock(VentaRepository.class);
        catalogoGateway = mock(CatalogoGateway.class);
        clientesGateway = mock(ClientesGateway.class);
        ventaService = new VentaService(
                ventaRepository, catalogoGateway, clientesGateway);

        when(clientesGateway.existeCliente(CEDULA_CLIENTE)).thenReturn(true);
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> {
            Venta v = inv.getArgument(0);
            v.setCodigoVenta(1L);
            return v;
        });
    }

    // ---------- helpers ----------

    private ProductoInfo producto(long codigo, String nombre, String precioVenta, String iva) {
        return new ProductoInfo(codigo, nombre,
                new BigDecimal(precioVenta), new BigDecimal(iva));
    }

    private void catalogoDevuelve(ProductoInfo... productos) {
        when(catalogoGateway.buscarProductos(anyList())).thenReturn(List.of(productos));
    }

    private RegistrarVentaRequest request(LineaVentaRequest... lineas) {
        return new RegistrarVentaRequest(CEDULA_CLIENTE, List.of(lineas));
    }

    private LineaVentaRequest linea(long codigo, int cantidad) {
        return new LineaVentaRequest(codigo, cantidad);
    }

    private void assertImporte(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real),
                () -> "esperado " + esperado + " pero fue " + real);
    }

    // ---------- tests ----------

    @Test
    @DisplayName("Venta con dos productos: calcula totales, IVA y total con IVA")
    void registrarCalculaTotales() {
        catalogoDevuelve(
                producto(10, "Arroz", "2500.00", "19.00"),
                producto(20, "Aceite", "8000.00", "19.00"));

        Venta venta = ventaService.registrarVenta(
                request(linea(10, 2), linea(20, 1)), CEDULA_USUARIO);

        // linea 10: 2500 x 2 = 5000 base ; IVA 5000 x 19% = 950
        // linea 20: 8000 x 1 = 8000 base ; IVA 8000 x 19% = 1520
        assertImporte("13000.00", venta.getTotalVenta());
        assertImporte("2470.00", venta.getValorIva());
        assertImporte("15470.00", venta.getTotalConIva());
        assertEquals(CEDULA_CLIENTE, venta.getCedulaCliente());
        assertEquals(CEDULA_USUARIO, venta.getCedulaUsuario());
        assertNotNull(venta.getFechaVenta());
        assertEquals(2, venta.getDetalles().size());
        verify(ventaRepository).save(any(Venta.class));
    }

    @Test
    @DisplayName("El IVA de linea se redondea HALF_UP a 2 decimales")
    void ivaLineaRedondeaHalfUp() {
        // 6.25 x 2 = 12.50 base ; IVA 12.50 x 19% = 2.375 -> 2.38
        catalogoDevuelve(producto(30, "Chicle", "6.25", "19.00"));

        Venta venta = ventaService.registrarVenta(request(linea(30, 2)), CEDULA_USUARIO);

        DetalleVenta detalle = venta.getDetalles().get(0);
        assertImporte("12.50", detalle.getValorTotal());
        assertImporte("2.38", detalle.getValorIva());
        assertImporte("2.38", venta.getValorIva());
        assertImporte("14.88", venta.getTotalConIva());
    }

    @Test
    @DisplayName("El detalle congela nombre, precio y % de IVA que da el catalogo, no el request")
    void detalleUsaSnapshotDelCatalogo() {
        catalogoDevuelve(producto(40, "Nombre Del Catalogo", "1234.56", "19.00"));

        Venta venta = ventaService.registrarVenta(request(linea(40, 1)), CEDULA_USUARIO);

        DetalleVenta detalle = venta.getDetalles().get(0);
        assertEquals(40L, detalle.getCodigoProducto());
        assertEquals("Nombre Del Catalogo", detalle.getNombreProducto());
        assertImporte("1234.56", detalle.getValorUnitario());
        assertImporte("19.00", detalle.getIvaPorcentaje());
        assertEquals(1, detalle.getCantidad());
    }

    @Test
    @DisplayName("Cliente inexistente en ms-clientes -> 404 y no persiste")
    void clienteInexistente() {
        when(clientesGateway.existeCliente(CEDULA_CLIENTE)).thenReturn(false);

        assertThrows(RecursoNoEncontradoException.class, () ->
                ventaService.registrarVenta(request(linea(10, 1)), CEDULA_USUARIO));
        verify(ventaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un codigo de producto que no esta en el catalogo -> 404 y no persiste")
    void productoInexistente() {
        catalogoDevuelve(producto(10, "Arroz", "2500.00", "19.00"));

        assertThrows(RecursoNoEncontradoException.class, () ->
                ventaService.registrarVenta(request(linea(10, 1), linea(99, 1)), CEDULA_USUARIO));
        verify(ventaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cantidad menor o igual a cero -> 409 y no persiste")
    void cantidadNoPositiva() {
        assertThrows(ReglaNegocioException.class, () ->
                ventaService.registrarVenta(request(linea(10, 0)), CEDULA_USUARIO));
        verify(ventaRepository, never()).save(any());
        verifyNoInteractions(catalogoGateway);
    }

    @Test
    @DisplayName("Venta sin lineas -> 409 y no persiste")
    void sinLineas() {
        RegistrarVentaRequest vacia = new RegistrarVentaRequest(CEDULA_CLIENTE, List.of());

        assertThrows(ReglaNegocioException.class, () ->
                ventaService.registrarVenta(vacia, CEDULA_USUARIO));
        verify(ventaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sin cedula de usuario (JWT sin claim) -> 409 y no persiste")
    void sinCedulaUsuario() {
        assertThrows(ReglaNegocioException.class, () ->
                ventaService.registrarVenta(request(linea(10, 1)), null));
        verify(ventaRepository, never()).save(any());
    }

    @Test
    @DisplayName("El mismo producto en dos lineas genera dos detalles y suma bien")
    void productoRepetidoEnDosLineas() {
        catalogoDevuelve(producto(10, "Arroz", "1000.00", "19.00"));

        Venta venta = ventaService.registrarVenta(
                request(linea(10, 1), linea(10, 3)), CEDULA_USUARIO);

        assertEquals(2, venta.getDetalles().size());
        assertImporte("4000.00", venta.getTotalVenta());
        assertImporte("760.00", venta.getValorIva());
        assertImporte("4760.00", venta.getTotalConIva());
    }
}
