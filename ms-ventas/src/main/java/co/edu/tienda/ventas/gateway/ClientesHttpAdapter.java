package co.edu.tienda.ventas.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Adaptador HTTP real de ClientesGateway. Reemplaza a la implementacion
 * provisional del Incremento 2.
 *
 * Verifica la existencia del cliente con POST /api/clientes/lote (ese endpoint
 * devuelve solo las cedulas que existen); si la respuesta viene vacia, el
 * cliente no existe. La llamada va autenticada con el JWT del usuario de sistema.
 */
@Component
public class ClientesHttpAdapter implements ClientesGateway {

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LISTA =
            new ParameterizedTypeReference<>() {};

    private final RestClient rest;
    private final AutenticadorServicio autenticador;
    private final String urlLote;

    public ClientesHttpAdapter(RestClient serviciosRestClient,
                               AutenticadorServicio autenticador,
                               @Value("${ms.clientes.base-url}") String clientesBaseUrl) {
        this.rest = serviciosRestClient;
        this.autenticador = autenticador;
        this.urlLote = clientesBaseUrl + "/api/clientes/lote";
    }

    @Override
    public boolean existeCliente(Long cedulaCliente) {
        List<Map<String, Object>> encontrados = autenticador.ejecutarAutenticado(
                "ms-clientes", token -> rest.post()
                        .uri(urlLote)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .body(List.of(cedulaCliente))
                        .retrieve()
                        .body(LISTA));

        return encontrados != null && !encontrados.isEmpty();
    }
}
