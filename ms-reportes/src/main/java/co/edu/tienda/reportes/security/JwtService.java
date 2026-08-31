package co.edu.tienda.reportes.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Version de solo lectura del JwtService: este microservicio VALIDA tokens,
 * pero nunca los emite. Solo ms-identidad puede emitirlos.
 *
 * Copia casi verbatim de la de los otros modulos (solo cambia el package).
 * Cualquier cambio en la logica de auth hay que replicarlo en los 5 modulos.
 */
@Service
public class JwtService {

    private final SecretKey clave;

    public JwtService(@Value("${app.jwt.secret}") String secreto) {
        this.clave = new SecretKeySpec(
                secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    public Claims validarYObtenerClaims(String token) {
        return Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
