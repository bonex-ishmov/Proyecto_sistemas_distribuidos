package co.edu.tienda.reportes.gateway;

import co.edu.tienda.reportes.dto.ClienteResponse;

import java.util.List;

/**
 * Puerto hacia ms-clientes. La implementación real (HTTP) es
 * {@link ClientesHttpAdapter}; el contrato queda aparte para poder testear
 * ReporteService con un mock.
 */
public interface ClientesGateway {

    /** Todos los clientes registrados en ms-clientes (GET /api/clientes). */
    List<ClienteResponse> listarClientes();

    /**
     * Datos de varios clientes en UNA sola llamada (POST /api/clientes/lote).
     * ms-clientes devuelve solo las cédulas que existen; las que no, se omiten.
     * Se usa para poner el nombre en el reporte de ventas por cliente.
     */
    List<ClienteResponse> buscarPorCedulas(List<Long> cedulas);
}
