package co.edu.tienda.reportes.gateway;

import co.edu.tienda.reportes.dto.ClienteResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Adaptador HTTP real de ClientesGateway.
 *
 * Todas las llamadas van contra ms-clientes autenticadas con el JWT del usuario
 * de sistema. El listado (GET /api/clientes) es un passthrough; el cruce de
 * nombres para el reporte de ventas usa POST /api/clientes/lote en UNA sola
 * llamada, nunca una petición por cédula.
 */
@Component
public class ClientesHttpAdapter implements ClientesGateway {

    private static final ParameterizedTypeReference<List<ClienteResponse>> LISTA =
            new ParameterizedTypeReference<>() {};

    private final RestClient rest;
    private final AutenticadorServicio autenticador;
    private final String urlClientes;
    private final String urlLote;

    public ClientesHttpAdapter(RestClient serviciosRestClient,
                               AutenticadorServicio autenticador,
                               @Value("${ms.clientes.base-url}") String clientesBaseUrl) {
        this.rest = serviciosRestClient;
        this.autenticador = autenticador;
        this.urlClientes = clientesBaseUrl + "/api/clientes";
        this.urlLote = clientesBaseUrl + "/api/clientes/lote";
    }

    @Override
    public List<ClienteResponse> listarClientes() {
        List<ClienteResponse> respuesta = autenticador.ejecutarAutenticado(
                "ms-clientes", token -> rest.get()
                        .uri(urlClientes)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve()
                        .body(LISTA));

        return respuesta == null ? List.of() : respuesta;
    }

    @Override
    public List<ClienteResponse> buscarPorCedulas(List<Long> cedulas) {
        if (cedulas == null || cedulas.isEmpty()) {
            return List.of();
        }

        List<ClienteResponse> respuesta = autenticador.ejecutarAutenticado(
                "ms-clientes", token -> rest.post()
                        .uri(urlLote)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .body(cedulas)
                        .retrieve()
                        .body(LISTA));

        return respuesta == null ? List.of() : respuesta;
    }
}
