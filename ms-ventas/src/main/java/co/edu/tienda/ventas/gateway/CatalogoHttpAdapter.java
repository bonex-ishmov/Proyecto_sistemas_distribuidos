package co.edu.tienda.ventas.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

/**
 * Adaptador HTTP real de CatalogoGateway. Reemplaza a la implementacion
 * provisional del Incremento 2.
 *
 * Pide los productos de la venta con POST /api/productos/lote en una sola
 * llamada, autenticada con el JWT del usuario de sistema, y traduce la
 * respuesta de ms-catalogo (ProductoResponse) al ProductoInfo que consume
 * VentaService: precio_venta -> valorUnitario, ivacompra -> ivaPorcentaje.
 */
@Component
public class CatalogoHttpAdapter implements CatalogoGateway {

    private static final ParameterizedTypeReference<List<ProductoCatalogo>> LISTA =
            new ParameterizedTypeReference<>() {};

    private final RestClient rest;
    private final AutenticadorServicio autenticador;
    private final String urlLote;

    public CatalogoHttpAdapter(RestClient serviciosRestClient,
                               AutenticadorServicio autenticador,
                               @Value("${ms.catalogo.base-url}") String catalogoBaseUrl) {
        this.rest = serviciosRestClient;
        this.autenticador = autenticador;
        this.urlLote = catalogoBaseUrl + "/api/productos/lote";
    }

    @Override
    public List<ProductoInfo> buscarProductos(List<Long> codigos) {
        if (codigos == null || codigos.isEmpty()) {
            return List.of();
        }

        List<ProductoCatalogo> respuesta = autenticador.ejecutarAutenticado(
                "ms-catalogo", token -> rest.post()
                        .uri(urlLote)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .body(codigos)
                        .retrieve()
                        .body(LISTA));

        if (respuesta == null) {
            return List.of();
        }
        return respuesta.stream()
                .map(p -> new ProductoInfo(
                        p.codigoProducto(), p.nombreProducto(),
                        p.precioVenta(), p.ivacompra()))
                .toList();
    }

    /** Subconjunto de ms-catalogo ProductoResponse: solo lo que ms-ventas usa. */
    record ProductoCatalogo(
            Long codigoProducto,
            String nombreProducto,
            BigDecimal precioVenta,
            BigDecimal ivacompra) {}
}
