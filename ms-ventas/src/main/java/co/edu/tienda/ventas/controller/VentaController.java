package co.edu.tienda.ventas.controller;

import co.edu.tienda.ventas.dto.RegistrarVentaRequest;
import co.edu.tienda.ventas.dto.VentaResponse;
import co.edu.tienda.ventas.exception.ReglaNegocioException;
import co.edu.tienda.ventas.mapper.VentaMapper;
import co.edu.tienda.ventas.service.VentaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/ventas")
public class VentaController {

    private final VentaService ventaService;

    public VentaController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    @PostMapping
    public ResponseEntity<VentaResponse> registrar(
            @Valid @RequestBody RegistrarVentaRequest request,
            Authentication authentication) {

        Long cedulaUsuario = extraerCedulaUsuario(authentication);
        VentaResponse creada = VentaMapper.aResponse(
                ventaService.registrarVenta(request, cedulaUsuario));

        return ResponseEntity
                .created(URI.create("/api/ventas/" + creada.codigoVenta()))
                .body(creada);
    }

    /**
     * La cedula la deja JwtAuthenticationFilter en auth.getDetails() a partir
     * del claim "cedula" del token. ms-ventas es el primer modulo que la usa.
     */
    private Long extraerCedulaUsuario(Authentication authentication) {
        Object detalle = authentication == null ? null : authentication.getDetails();
        if (!(detalle instanceof Number cedula)) {
            throw new ReglaNegocioException("El token no contiene la cedula del usuario");
        }
        return cedula.longValue();
    }
}
