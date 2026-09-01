package co.edu.tienda.reportes.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Adaptador HTTP real de VentasGateway.
 *
 * Hace GET /api/ventas/total-por-cliente contra ms-ventas, autenticado con el
 * JWT del usuario de sistema. ms-ventas ya hace la agregación en la BD y ordena
 * por cédula; ms-reportes solo le añade el nombre del cliente.
 */
@Component
public class VentasHttpAdapter implements VentasGateway {

    private static final ParameterizedTypeReference<List<TotalVentasCliente>> LISTA =
            new ParameterizedTypeReference<>() {};

    private final RestClient rest;
    private final AutenticadorServicio autenticador;
    private final String urlTotalPorCliente;

    public VentasHttpAdapter(RestClient serviciosRestClient,
                             AutenticadorServicio autenticador,
                             @Value("${ms.ventas.base-url}") String ventasBaseUrl) {
        this.rest = serviciosRestClient;
        this.autenticador = autenticador;
        this.urlTotalPorCliente = ventasBaseUrl + "/api/ventas/total-por-cliente";
    }

    @Override
    public List<TotalVentasCliente> totalPorCliente() {
        List<TotalVentasCliente> respuesta = autenticador.ejecutarAutenticado(
                "ms-ventas", token -> rest.get()
                        .uri(urlTotalPorCliente)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve()
                        .body(LISTA));

        return respuesta == null ? List.of() : respuesta;
    }
}
