package co.edu.tienda.reportes.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Adaptador HTTP real de CatalogoGateway.
 *
 * GET /api/productos y GET /api/proveedores contra ms-catalogo, autenticados con
 * el JWT del usuario de sistema. Solo interesa CUANTOS hay, no sus campos: se
 * deserializa a List&lt;Object&gt; y se devuelve el tamano.
 */
@Component
public class CatalogoHttpAdapter implements CatalogoGateway {

    private static final ParameterizedTypeReference<List<Object>> LISTA =
            new ParameterizedTypeReference<>() {};

    private final RestClient rest;
    private final AutenticadorServicio autenticador;
    private final String urlProductos;
    private final String urlProveedores;

    public CatalogoHttpAdapter(RestClient serviciosRestClient,
                               AutenticadorServicio autenticador,
                               @Value("${ms.catalogo.base-url}") String catalogoBaseUrl) {
        this.rest = serviciosRestClient;
        this.autenticador = autenticador;
        this.urlProductos = catalogoBaseUrl + "/api/productos";
        this.urlProveedores = catalogoBaseUrl + "/api/proveedores";
    }

    @Override
    public long contarProductos() {
        return contar(urlProductos);
    }

    @Override
    public long contarProveedores() {
        return contar(urlProveedores);
    }

    private long contar(String url) {
        List<Object> respuesta = autenticador.ejecutarAutenticado(
                "ms-catalogo", token -> rest.get()
                        .uri(url)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve()
                        .body(LISTA));

        return respuesta == null ? 0L : respuesta.size();
    }
}
