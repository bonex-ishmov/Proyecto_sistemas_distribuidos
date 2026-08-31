package co.edu.tienda.ventas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Un unico RestClient compartido por AutenticadorServicio y por los adaptadores
 * HTTP hacia ms-identidad, ms-clientes y ms-catalogo.
 *
 * No lleva baseUrl: cada adaptador arma la URL absoluta con su propiedad
 * ms.<servicio>.base-url. Tener un solo punto de construccion es lo que permite,
 * en el test end-to-end, interceptar TODO el trafico saliente con un
 * MockRestServiceServer atado a este mismo builder.
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
