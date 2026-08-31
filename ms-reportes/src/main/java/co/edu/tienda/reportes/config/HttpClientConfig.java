package co.edu.tienda.reportes.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Un unico RestClient compartido por AutenticadorServicio y por los adaptadores
 * HTTP hacia ms-identidad, ms-clientes y ms-ventas.
 *
 * No lleva baseUrl: cada adaptador arma la URL absoluta con su propiedad
 * ms.&lt;servicio&gt;.base-url. Tener un solo punto de construccion es lo que
 * permite, en el test end-to-end, interceptar TODO el trafico saliente con un
 * MockRestServiceServer atado a este mismo builder.
 *
 * Copia verbatim de la de ms-ventas (solo cambia el package).
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public RestClient serviciosRestClient() {
        // RestClient.builder() sin argumentos: usa el cliente HTTP del JDK
        // (java.net.http). No dependemos del bean RestClient.Builder porque los
        // starters actuales no lo autoconfiguran.
        return RestClient.builder().build();
    }
}
