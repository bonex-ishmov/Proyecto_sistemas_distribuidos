package co.edu.tienda.reportes.gateway;

import co.edu.tienda.reportes.dto.UsuarioResponse;

import java.util.List;

/**
 * Puerto hacia ms-identidad. La implementación real (HTTP) es
 * {@link IdentidadHttpAdapter}; el contrato queda aparte para poder testear
 * ReporteService con un mock.
 */
public interface IdentidadGateway {

    /** Todos los usuarios registrados en ms-identidad (GET /api/usuarios). */
    List<UsuarioResponse> listarUsuarios();
}
