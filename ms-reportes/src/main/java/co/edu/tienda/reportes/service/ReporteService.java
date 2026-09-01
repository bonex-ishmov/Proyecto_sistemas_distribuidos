package co.edu.tienda.reportes.service;

import co.edu.tienda.reportes.dto.ClienteResponse;
import co.edu.tienda.reportes.dto.UsuarioResponse;
import co.edu.tienda.reportes.dto.VentasPorClienteResponse;
import co.edu.tienda.reportes.gateway.ClientesGateway;
import co.edu.tienda.reportes.gateway.IdentidadGateway;
import co.edu.tienda.reportes.gateway.TotalVentasCliente;
import co.edu.tienda.reportes.gateway.VentasGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Lógica del módulo 7. ms-reportes no tiene base de datos: cada consulta se
 * arma pidiendo datos por HTTP a otros microservicios (vía los gateways) y
 * combinándolos.
 */
@Service
public class ReporteService {

    /** Nombre que se muestra cuando el cliente de una venta ya no existe en ms-clientes. */
    static final String NOMBRE_CLIENTE_DESCONOCIDO = "(cliente eliminado)";

    private final IdentidadGateway identidadGateway;
    private final ClientesGateway clientesGateway;
    private final VentasGateway ventasGateway;

    /**
     * Cédula del usuario de sistema (compartido con ms-ventas) que se oculta del
     * listado de usuarios: es una cuenta técnica de autenticación
     * servicio-a-servicio, no un usuario real de la tienda. admininicial
     * (cédula 1) SÍ aparece: es un usuario real aunque esté desactivado.
     */
    private final long cedulaUsuarioSistema;

    public ReporteService(IdentidadGateway identidadGateway,
                          ClientesGateway clientesGateway,
                          VentasGateway ventasGateway,
                          @Value("${app.reportes.cedula-usuario-sistema:2}") long cedulaUsuarioSistema) {
        this.identidadGateway = identidadGateway;
        this.clientesGateway = clientesGateway;
        this.ventasGateway = ventasGateway;
        this.cedulaUsuarioSistema = cedulaUsuarioSistema;
    }

    /**
     * Listado de usuarios (módulo 7, consulta a). Todos los de ms-identidad
     * menos el usuario de sistema.
     */
    public List<UsuarioResponse> listarUsuarios() {
        return identidadGateway.listarUsuarios().stream()
                .filter(u -> u.cedulaUsuario() == null
                        || u.cedulaUsuario() != cedulaUsuarioSistema)
                .toList();
    }

    /**
     * Listado de clientes (módulo 7, consulta b). Passthrough de lo que
     * devuelve ms-clientes.
     */
    public List<ClienteResponse> listarClientes() {
        return clientesGateway.listarClientes();
    }

    /**
     * Total de ventas por cliente (módulo 7, consulta c). Solo clientes con al
     * menos una venta (eso ya lo garantiza ms-ventas). El nombre se resuelve con
     * UNA sola llamada a POST /api/clientes/lote con todas las cédulas.
     */
    public List<VentasPorClienteResponse> ventasPorCliente() {
        List<TotalVentasCliente> totales = ventasGateway.totalPorCliente();
        if (totales.isEmpty()) {
            return List.of();
        }

        List<Long> cedulas = totales.stream()
                .map(TotalVentasCliente::cedulaCliente)
                .toList();

        Map<Long, String> nombrePorCedula = clientesGateway.buscarPorCedulas(cedulas).stream()
                .collect(Collectors.toMap(
                        ClienteResponse::cedulaCliente,
                        ClienteResponse::nombreCliente,
                        (a, b) -> a));

        // Se conserva el orden que entrega ms-ventas (por cédula).
        return totales.stream()
                .map(t -> new VentasPorClienteResponse(
                        t.cedulaCliente(),
                        nombrePorCedula.getOrDefault(t.cedulaCliente(), NOMBRE_CLIENTE_DESCONOCIDO),
                        t.totalConIva()))
                .toList();
    }
}
