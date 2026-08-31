package co.edu.tienda.reportes.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Sin esto, Spring Security responde 403 a las peticiones sin token.
 * Lo correcto es 401: "no se quien eres".
 *
 * Copia casi verbatim de la de los otros modulos (solo cambia el package).
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String json = """
                {"timestamp":"%s","status":401,"error":"UNAUTHORIZED",\
                "mensaje":"Token ausente, invalido o expirado","path":"%s"}"""
                .formatted(LocalDateTime.now(), request.getRequestURI());

        response.getWriter().write(json);
    }
}
