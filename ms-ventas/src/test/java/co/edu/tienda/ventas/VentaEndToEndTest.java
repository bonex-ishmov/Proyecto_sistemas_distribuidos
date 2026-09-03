package co.edu.tienda.ventas;

import co.edu.tienda.ventas.entity.Venta;
import co.edu.tienda.ventas.repository.VentaRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba end-to-end del Incremento 4: entra por HTTP a POST /api/ventas con un
 * JWT de usuario, pasa por seguridad, servicio y los adaptadores HTTP reales
 * (AutenticadorServicio + CatalogoHttpAdapter + ClientesHttpAdapter), persiste
 * en H2 y responde 201.
 *
 * El trafico saliente hacia ms-identidad / ms-clientes / ms-catalogo lo
 * intercepta un MockRestServiceServer atado al mismo RestClient.Builder que usa
 * el servicio, asi que se ejercita la serializacion real de las peticiones sin
 * levantar los otros microservicios.
 */
@SpringBootTest
@ActiveProfiles("test")
class VentaEndToEndTest {

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
    private VentaRepository ventaRepository;
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
        ventaRepository.deleteAll();
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
    @DisplayName("POST /api/ventas: login de sistema + lote de clientes + lote de catalogo, y persiste la venta")
    void registraVentaPorLosAdaptadoresHttpReales() throws Exception {

        servidor.expect(MockRestRequestMatchers.requestTo("http://ms-identidad.test/api/auth/login"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.jsonPath("$.usuario").value("ventas-sistema"))
                .andExpect(MockRestRequestMatchers.jsonPath("$.password").value("clave-de-prueba"))
                .andRespond(withSuccess("""
                        {"token":"token-de-sistema","tipo":"Bearer","cedulaUsuario":9,
                         "usuario":"ventas-sistema","nombreUsuario":"Ventas Sistema",
                         "expiraEnMs":3600000}
                        """, MediaType.APPLICATION_JSON));

        servidor.expect(MockRestRequestMatchers.requestTo("http://ms-clientes.test/api/clientes/lote"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header("Authorization", "Bearer token-de-sistema"))
                .andExpect(MockRestRequestMatchers.content().json("[222]"))
                .andRespond(withSuccess("""
                        [{"cedulaCliente":222,"nombreCliente":"Ana Ruiz",
                          "direccionCliente":"Calle 1","telefonoCliente":"3000000",
                          "emailCliente":"ana@x.com"}]
                        """, MediaType.APPLICATION_JSON));

        servidor.expect(MockRestRequestMatchers.requestTo("http://ms-catalogo.test/api/productos/lote"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header("Authorization", "Bearer token-de-sistema"))
                .andExpect(MockRestRequestMatchers.content().json("[10]"))
                .andRespond(withSuccess("""
                        [{"codigoProducto":10,"nombreProducto":"Arroz Diana 500g",
                          "nitproveedor":1,"nombreProveedor":"Distribuidora X",
                          "precioCompra":2100.00,"ivacompra":19.00,"precioVenta":2500.00}]
                        """, MediaType.APPLICATION_JSON));

        mockMvc.perform(post("/api/ventas")
                        .header("Authorization", "Bearer " + jwtDeUsuario(111))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cedulaCliente":222,
                                 "detalles":[{"codigoProducto":10,"cantidad":2}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.codigoVenta").isNumber())
                .andExpect(jsonPath("$.cedulaCliente").value(222))
                .andExpect(jsonPath("$.cedulaUsuario").value(111))
                .andExpect(jsonPath("$.detalles.length()").value(1))
                .andExpect(jsonPath("$.detalles[0].nombreProducto").value("Arroz Diana 500g"))
                .andExpect(jsonPath("$.detalles[0].valorUnitario").value(2500.00));

        servidor.verify();

        List<Venta> ventas = ventaRepository.findAll();
        assertThat(ventas).hasSize(1);
        Venta venta = ventas.get(0);
        assertThat(venta.getCedulaCliente()).isEqualTo(222L);
        assertThat(venta.getCedulaUsuario()).isEqualTo(111L);
        assertThat(venta.getTotalVenta()).isEqualByComparingTo("5000.00");
        assertThat(venta.getValorIva()).isEqualByComparingTo("950.00");
        assertThat(venta.getTotalConIva()).isEqualByComparingTo("5950.00");
        assertThat(venta.getDetalles()).hasSize(1);
        assertThat(venta.getDetalles().get(0).getNombreProducto()).isEqualTo("Arroz Diana 500g");
    }

    @Test
    @DisplayName("GET /api/ventas/resumen: ejecuta las dos consultas agregadas contra la BD; "
            + "tabla vacia -> hoy y mes en 0 / 0.00")
    void resumenEjecutaLasConsultasAgregadas() throws Exception {
        // @BeforeEach ya hizo deleteAll(): sin ventas, COUNT = 0 y SUM = null, que
        // VentaService normaliza a 0.00. Resultado deterministico, sin depender de
        // la zona horaria del JVM que corre el test.
        mockMvc.perform(get("/api/ventas/resumen")
                        .header("Authorization", "Bearer " + jwtDeUsuario(111)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hoy.cantidad").value(0))
                .andExpect(jsonPath("$.hoy.montoTotalConIva").value(0.00))
                .andExpect(jsonPath("$.mes.cantidad").value(0))
                .andExpect(jsonPath("$.mes.montoTotalConIva").value(0.00));
    }
}
