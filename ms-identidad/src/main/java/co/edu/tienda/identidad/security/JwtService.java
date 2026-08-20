package co.edu.tienda.identidad.security;

import co.edu.tienda.identidad.entity.Usuario;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import io.jsonwebtoken.Claims;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Responsable de construir y firmar el token JWT.
 * Un JWT tiene 3 partes: cabecera.payload.firma
 * El payload NO va cifrado (es Base64), por eso nunca metemos ahí la contraseña.
 */
@Service
public class JwtService {

    private final SecretKey clave;
    private final long expiracionMs;

    public JwtService(@Value("${app.jwt.secret}") String secreto,
                      @Value("${app.jwt.expiracion-ms}") long expiracionMs) {
        this.clave = new SecretKeySpec(
                secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.expiracionMs = expiracionMs;
    }

    public String generarToken(Usuario usuario) {
        Date ahora = new Date();
        Date vencimiento = new Date(ahora.getTime() + expiracionMs);

        return Jwts.builder()
                .subject(usuario.getUsuario())
                .claim("cedula", usuario.getCedulaUsuario())
                .claim("nombre", usuario.getNombreUsuario())
                .issuedAt(ahora)
                .expiration(vencimiento)
                .signWith(clave)
                .compact();
    }

    public long getExpiracionMs() {
        return expiracionMs;
    }

    public Claims validarYObtenerClaims(String token) {
        return Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}