package co.edu.tienda.reportes.service;

import co.edu.tienda.reportes.dto.ClienteResponse;
import co.edu.tienda.reportes.dto.UsuarioResponse;
import co.edu.tienda.reportes.dto.VentasPorClienteResponse;
import co.edu.tienda.reportes.gateway.ClientesGateway;
import co.edu.tienda.reportes.gateway.IdentidadGateway;
import co.edu.tienda.reportes.gateway.TotalVentasCliente;
import co.edu.tienda.reportes.gateway.VentasGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unitario de la capa service, mismo estilo que el resto del monorepo:
 * JUnit 5 + Mockito, sin contexto Spring. Los gateways (ms-identidad,
 * ms-clientes, ms-ventas) van mockeados; el HTTP real y su prueba end-to-end
 * llegan en el Incremento 4.
 */
class ReporteServiceTest {

    private static final long CEDULA_USUARIO_SISTEMA = 2L;

    private IdentidadGateway identidadGateway;
    private ClientesGateway clientesGateway;
    private VentasGateway ventasGateway;
    private ReporteService reporteService;

    @BeforeEach
    void preparar() {
        identidadGateway = mock(IdentidadGateway.class);
        clientesGateway = mock(ClientesGateway.class);
        ventasGateway = mock(VentasGateway.class);
        reporteService = new ReporteService(
                identidadGateway, clientesGateway, ventasGateway, CEDULA_USUARIO_SISTEMA);
    }

    private static UsuarioResponse usuario(long cedula, String nombreUsuario, boolean activo) {
        return new UsuarioResponse(cedula, nombreUsuario, nombreUsuario + "@x.com",
                nombreUsuario, activo);
    }

    private static ClienteResponse cliente(long cedula, String nombre) {
        return new ClienteResponse(cedula, nombre, "Calle 1", "3000000",
                nombre.replace(" ", "") + "@x.com");
    }

    // ---------- listarUsuarios ----------

    @Test
    @DisplayName("listarUsuarios: oculta la cedula 2 y conserva admininicial (cedula 1)")
    void listarUsuariosOcultaSoloElUsuarioDeSistema() {
        when(identidadGateway.listarUsuarios()).thenReturn(List.of(
                usuario(1L, "admininicial", false),   // real aunque desactivado -> aparece
                usuario(2L, "ventas-sistema", true),  // usuario de sistema -> se oculta
                usuario(50L, "cajera01", true)
        ));

        List<UsuarioResponse> resultado = reporteService.listarUsuarios();

        assertEquals(2, resultado.size());
        assertTrue(resultado.stream().anyMatch(u -> u.cedulaUsuario() == 1L),
                "admininicial debe aparecer en el listado");
        assertTrue(resultado.stream().noneMatch(u -> u.cedulaUsuario() == 2L),
                "el usuario de sistema no debe aparecer");
        assertTrue(resultado.stream().anyMatch(u -> u.cedulaUsuario() == 50L));
    }

    @Test
    @DisplayName("listarUsuarios: si ms-identidad no trae el usuario de sistema, devuelve todo")
    void listarUsuariosSinUsuarioDeSistemaDevuelveTodo() {
        when(identidadGateway.listarUsuarios()).thenReturn(List.of(
                usuario(1L, "admininicial", false),
                usuario(50L, "cajera01", true)
        ));

        assertEquals(2, reporteService.listarUsuarios().size());
    }

    @Test
    @DisplayName("listarUsuarios: un usuario con cedula null no rompe el filtro y se conserva")
    void listarUsuariosToleraCedulaNull() {
        when(identidadGateway.listarUsuarios()).thenReturn(List.of(
                new UsuarioResponse(null, "raro", "raro@x.com", "raro", true),
                usuario(2L, "ventas-sistema", true)
        ));

        List<UsuarioResponse> resultado = reporteService.listarUsuarios();

        assertEquals(1, resultado.size());
        assertEquals("raro", resultado.get(0).usuario());
    }

    // ---------- listarClientes ----------

    @Test
    @DisplayName("listarClientes: passthrough de lo que devuelve ms-clientes")
    void listarClientesEsPassthrough() {
        List<ClienteResponse> clientes = List.of(cliente(222L, "Ana Ruiz"));
        when(clientesGateway.listarClientes()).thenReturn(clientes);

        assertSame(clientes, reporteService.listarClientes());
    }

    @Test
    @DisplayName("listarClientes: lista vacia se devuelve tal cual")
    void listarClientesVacia() {
        when(clientesGateway.listarClientes()).thenReturn(List.of());

        assertTrue(reporteService.listarClientes().isEmpty());
    }

    // ---------- ventasPorCliente ----------

    @Test
    @DisplayName("ventasPorCliente: cruza nombres con UNA sola llamada al lote y conserva el orden de ms-ventas")
    void ventasPorClienteCruzaNombresEnUnaLlamada() {
        when(ventasGateway.totalPorCliente()).thenReturn(List.of(
                new TotalVentasCliente(222L, new BigDecimal("5950.00")),
                new TotalVentasCliente(333L, new BigDecimal("1190.00"))
        ));
        when(clientesGateway.buscarPorCedulas(List.of(222L, 333L))).thenReturn(List.of(
                cliente(333L, "Beto Diaz"),
                cliente(222L, "Ana Ruiz")
        ));

        List<VentasPorClienteResponse> resultado = reporteService.ventasPorCliente();

        verify(clientesGateway, times(1)).buscarPorCedulas(anyList());
        assertEquals(2, resultado.size());
        // orden = el de ms-ventas, no el de la respuesta del lote
        assertEquals(222L, resultado.get(0).cedulaCliente());
        assertEquals("Ana Ruiz", resultado.get(0).nombreCliente());
        assertEquals(new BigDecimal("5950.00"), resultado.get(0).totalConIva());
        assertEquals(333L, resultado.get(1).cedulaCliente());
        assertEquals("Beto Diaz", resultado.get(1).nombreCliente());
    }

    @Test
    @DisplayName("ventasPorCliente: pasa exactamente las cedulas de ms-ventas al lote")
    void ventasPorClienteEnviaLasCedulasDeVentas() {
        when(ventasGateway.totalPorCliente()).thenReturn(List.of(
                new TotalVentasCliente(10L, new BigDecimal("100.00")),
                new TotalVentasCliente(20L, new BigDecimal("200.00"))
        ));
        when(clientesGateway.buscarPorCedulas(anyList())).thenReturn(List.of());

        reporteService.ventasPorCliente();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        verify(clientesGateway).buscarPorCedulas(captor.capture());
        assertEquals(List.of(10L, 20L), captor.getValue());
    }

    @Test
    @DisplayName("ventasPorCliente: sin ventas devuelve lista vacia y NO llama a ms-clientes")
    void ventasPorClienteSinVentas() {
        when(ventasGateway.totalPorCliente()).thenReturn(List.of());

        assertTrue(reporteService.ventasPorCliente().isEmpty());
        verify(clientesGateway, never()).buscarPorCedulas(anyList());
    }

    @Test
    @DisplayName("ventasPorCliente: cliente borrado tras vender -> nombre '(cliente eliminado)'")
    void ventasPorClienteClienteBorrado() {
        when(ventasGateway.totalPorCliente()).thenReturn(List.of(
                new TotalVentasCliente(999L, new BigDecimal("4200.00"))
        ));
        when(clientesGateway.buscarPorCedulas(List.of(999L))).thenReturn(List.of()); // ya no existe

        List<VentasPorClienteResponse> resultado = reporteService.ventasPorCliente();

        assertEquals(1, resultado.size());
        assertEquals(999L, resultado.get(0).cedulaCliente());
        assertEquals("(cliente eliminado)", resultado.get(0).nombreCliente());
        assertEquals(new BigDecimal("4200.00"), resultado.get(0).totalConIva());
    }
}
