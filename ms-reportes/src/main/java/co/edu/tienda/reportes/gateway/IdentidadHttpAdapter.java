package co.edu.tienda.reportes.gateway;

import co.edu.tienda.reportes.dto.UsuarioResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Adaptador HTTP real de IdentidadGateway.
 *
 * Hace GET /api/usuarios contra ms-identidad, autenticado con el JWT del usuario
 * de sistema (el mismo que usa ms-ventas). Devuelve TODOS los usuarios; el
 * filtrado del usuario de sistema lo hace ReporteService, no este adaptador.
 */
@Component
public class IdentidadHttpAdapter implements IdentidadGateway {

    private static final ParameterizedTypeReference<List<UsuarioResponse>> LISTA =
            new ParameterizedTypeReference<>() {};

    private final RestClient rest;
    private final AutenticadorServicio autenticador;
    private final String urlUsuarios;

    public IdentidadHttpAdapter(RestClient serviciosRestClient,
                                AutenticadorServicio autenticador,
                                @Value("${ms.identidad.base-url}") String identidadBaseUrl) {
        this.rest = serviciosRestClient;
        this.autenticador = autenticador;
        this.urlUsuarios = identidadBaseUrl + "/api/usuarios";
    }

    @Override
    public List<UsuarioResponse> listarUsuarios() {
        List<UsuarioResponse> respuesta = autenticador.ejecutarAutenticado(
                "ms-identidad", token -> rest.get()
                        .uri(urlUsuarios)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve()
                        .body(LISTA));

        return respuesta == null ? List.of() : respuesta;
    }
}
