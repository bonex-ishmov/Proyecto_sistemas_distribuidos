package co.edu.tienda.clientes.security;

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
 * Sin esto, Spring Security responde 403 a las peticiones sin token,
 * lo cual confunde: 403 significa "te conozco pero no puedes",
 * y lo correcto aquí es 401 "no sé quién eres".
 *
 * El JSON se arma a mano a propósito: así no dependemos de qué versión
 * de Jackson esté en el classpath.
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
                "mensaje":"Token ausente, inválido o expirado","path":"%s"}"""
                .formatted(LocalDateTime.now(), request.getRequestURI());

        response.getWriter().write(json);
    }
}