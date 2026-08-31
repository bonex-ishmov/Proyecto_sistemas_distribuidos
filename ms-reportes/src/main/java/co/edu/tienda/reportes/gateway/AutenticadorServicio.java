package co.edu.tienda.reportes.gateway;

import co.edu.tienda.reportes.exception.DependenciaNoDisponibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.function.Function;

/**
 * Autenticacion servicio-a-servicio (decision de CLAUDE.md: sin API Gateway).
 * ms-reportes reutiliza el MISMO usuario de sistema que ms-ventas
 * ('ventas-sistema', cedula 2): solo hace lecturas, no necesita identidad
 * propia. Hace login contra ms-identidad y reutiliza ese JWT para llamar a los
 * endpoints de ms-identidad, ms-clientes y ms-ventas, que siguen exigiendo token.
 *
 * El login es PEREZOSO: lo dispara la primera llamada que necesita token, no el
 * arranque del servicio. El token se cachea hasta poco antes de expirar y se
 * renueva solo. Si una llamada autenticada recibe 401, se invalida el token en
 * cache y se reintenta una unica vez.
 *
 * Copia casi verbatim de la de ms-ventas (solo cambia el package).
 */
@Component
public class AutenticadorServicio {

    private static final Logger log = LoggerFactory.getLogger(AutenticadorServicio.class);

    /** Margen para renovar antes de que el token expire de verdad. */
    private static final long MARGEN_RENOVACION_MS = 60_000L;

    private final RestClient rest;
    private final String urlLogin;
    private final String usuarioSistema;
    private final String passwordSistema;

    private String token;
    private long renovarDespuesDe;

    public AutenticadorServicio(
            RestClient serviciosRestClient,
            @Value("${ms.identidad.base-url}") String identidadBaseUrl,
            @Value("${ms.identidad.usuario-sistema}") String usuarioSistema,
            @Value("${ms.identidad.password-sistema}") String passwordSistema) {
        this.rest = serviciosRestClient;
        this.urlLogin = identidadBaseUrl + "/api/auth/login";
        this.usuarioSistema = usuarioSistema;
        this.passwordSistema = passwordSistema;
    }

    /**
     * Ejecuta una llamada HTTP pasandole un Bearer token valido. Ante un 401
     * renueva el token y reintenta una vez; cualquier otro fallo de red o HTTP
     * se traduce a DependenciaNoDisponibleException (503).
     */
    public <T> T ejecutarAutenticado(String servicio, Function<String, T> llamada) {
        try {
            return llamada.apply(token());
        } catch (HttpClientErrorException.Unauthorized ex) {
            log.warn("{} respondio 401; renovando token de servicio y reintentando", servicio);
            invalidar();
            try {
                return llamada.apply(token());
            } catch (RestClientException reintento) {
                throw new DependenciaNoDisponibleException(
                        "No fue posible comunicarse con " + servicio, reintento);
            }
        } catch (RestClientException ex) {
            throw new DependenciaNoDisponibleException(
                    "No fue posible comunicarse con " + servicio, ex);
        }
    }

    synchronized String token() {
        if (token == null || System.currentTimeMillis() >= renovarDespuesDe) {
            autenticar();
        }
        return token;
    }

    synchronized void invalidar() {
        token = null;
    }

    private void autenticar() {
        try {
            RespuestaLogin respuesta = rest.post()
                    .uri(urlLogin)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new PeticionLogin(usuarioSistema, passwordSistema))
                    .retrieve()
                    .body(RespuestaLogin.class);

            if (respuesta == null || respuesta.token() == null) {
                throw new DependenciaNoDisponibleException(
                        "ms-identidad no devolvio token para el usuario de sistema");
            }
            this.token = respuesta.token();
            long ttl = Math.max(0L, respuesta.expiraEnMs() - MARGEN_RENOVACION_MS);
            this.renovarDespuesDe = System.currentTimeMillis() + ttl;
            log.info("Usuario de sistema '{}' autenticado contra ms-identidad", usuarioSistema);

        } catch (RestClientException ex) {
            throw new DependenciaNoDisponibleException(
                    "No fue posible autenticar el usuario de sistema contra ms-identidad", ex);
        }
    }

    /** Cuerpo del POST /api/auth/login. */
    record PeticionLogin(String usuario, String password) {}

    /** Subconjunto de LoginResponse de ms-identidad: solo lo que se usa aca. */
    record RespuestaLogin(String token, long expiraEnMs) {}
}
