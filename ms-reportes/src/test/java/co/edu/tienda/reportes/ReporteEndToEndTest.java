package co.edu.tienda.reportes;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.WebApplicationContext;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba end-to-end del Incremento 4: entra por HTTP a
 * GET /api/reportes/ventas-por-cliente con un JWT de usuario, pasa por
 * seguridad, servicio y los tres adaptadores HTTP reales
 * (AutenticadorServicio + VentasHttpAdapter + ClientesHttpAdapter).
 *
 * ms-reportes no tiene base de datos, asi que a diferencia del equivalente de
 * ms-ventas (VentaEndToEndTest) aca no hay repositorio que limpiar ni
 * persistencia que verificar: la prueba es enteramente de orquestacion HTTP.
 *
 * El trafico saliente hacia ms-identidad / ms-clientes / ms-ventas lo
 * intercepta un MockRestServiceServer atado al mismo RestClient.Builder que usa
 * el servicio, asi que se ejercita la serializacion real de las peticiones sin
 * levantar los otros microservicios.
 */
@SpringBootTest
@ActiveProfiles("test")
class ReporteEndToEndTest {

    /**
     * Reemplaza el bean serviciosRestClient de HttpClientConfig por uno atado a
     * un MockRestServiceServer. El builder es un campo compartido: el bean del
     * server lo configura y luego el RestClient se construye del mismo builder.
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class HttpMockConfig {

        private final RestClient.Builder builder = RestClient.builder();

        @Bean
        MockRestServiceServer mockRestServiceServer() {
            return MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        }

        @Bean
        RestClient serviciosRestClient(MockRestServiceServer server) {
            return builder.build();
        }
    }

    @Autowired
    private WebApplicationContext contexto;
    @Autowired
    private FilterChainProxy springSecurityFilterChain;
    @Autowired
    private MockRestServiceServer servidor;
    @Value("${app.jwt.secret}")
    private String secreto;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        // MockMvc armado a mano: en Boot 4.1 @AutoConfigureMockMvc vive en un
        // modulo que spring-boot-starter-test ya no arrastra. Se le enchufa la
        // cadena de filtros de Spring Security para que el JWT se valide de verdad.
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto)
                .addFilters(springSecurityFilterChain)
                .build();
        servidor.reset();
    }

    private String jwtDeUsuario(long cedula) {
        SecretKey clave = new SecretKeySpec(
                secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return Jwts.builder()
                .subject("cajera01")
                .claim("cedula", cedula)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000L))
                .signWith(clave)
                .compact();
    }

    @Test
    @DisplayName("GET /api/reportes/ventas-por-cliente: login de sistema + total por cliente + lote de "
            + "clientes en UNA sola llamada, y cruza los nombres")
    void ventasPorClientePorLosAdaptadoresHttpReales() throws Exception {

        servidor.expect(MockRestRequestMatchers.requestTo("http://ms-identidad.test/api/auth/login"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.jsonPath("$.usuario").value("ventas-sistema"))
                .andExpect(MockRestRequestMatchers.jsonPath("$.password").value("clave-de-prueba"))
                .andRespond(withSuccess("""
                        {"token":"token-de-sistema","tipo":"Bearer","cedulaUsuario":2,
                         "usuario":"ventas-sistema","nombreUsuario":"Ventas Sistema",
                         "expiraEnMs":3600000}
                        """, MediaType.APPLICATION_JSON));

        servidor.expect(MockRestRequestMatchers.requestTo("http://ms-ventas.test/api/ventas/total-por-cliente"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header("Authorization", "Bearer token-de-sistema"))
                .andRespond(withSuccess("""
                        [{"cedulaCliente":222,"totalConIva":5950.00},
                         {"cedulaCliente":333,"totalConIva":1190.00}]
                        """, MediaType.APPLICATION_JSON));

        servidor.expect(MockRestRequestMatchers.requestTo("http://ms-clientes.test/api/clientes/lote"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header("Authorization", "Bearer token-de-sistema"))
                .andExpect(MockRestRequestMatchers.content().json("[222,333]"))
                .andRespond(withSuccess("""
                        [{"cedulaCliente":333,"nombreCliente":"Beto Diaz",
                          "direccionCliente":"Calle 2","telefonoCliente":"3000001",
                          "emailCliente":"beto@x.com"},
                         {"cedulaCliente":222,"nombreCliente":"Ana Ruiz",
                          "direccionCliente":"Calle 1","telefonoCliente":"3000000",
                          "emailCliente":"ana@x.com"}]
                        """, MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/api/reportes/ventas-por-cliente")
                        .header("Authorization", "Bearer " + jwtDeUsuario(111)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                // orden = el de ms-ventas, no el de la respuesta del lote
                .andExpect(jsonPath("$[0].cedulaCliente").value(222))
                .andExpect(jsonPath("$[0].nombreCliente").value("Ana Ruiz"))
                .andExpect(jsonPath("$[0].totalConIva").value(5950.00))
                .andExpect(jsonPath("$[1].cedulaCliente").value(333))
                .andExpect(jsonPath("$[1].nombreCliente").value("Beto Diaz"))
                .andExpect(jsonPath("$[1].totalConIva").value(1190.00));

        servidor.verify();
    }
}
