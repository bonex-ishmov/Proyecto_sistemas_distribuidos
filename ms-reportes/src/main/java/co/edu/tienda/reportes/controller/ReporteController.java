package co.edu.tienda.reportes.controller;

import co.edu.tienda.reportes.dto.ClienteResponse;
import co.edu.tienda.reportes.dto.UsuarioResponse;
import co.edu.tienda.reportes.dto.VentasPorClienteResponse;
import co.edu.tienda.reportes.service.ReporteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Módulo 7: consultas y reportes. Toda ruta exige un JWT válido de usuario
 * (lo verifica SecurityConfig); ms-reportes lo reenvía a los otros servicios
 * usando el token del usuario de sistema, no el del llamante.
 */
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    /** Consulta a: listado de usuarios (sin el usuario de sistema). */
    @GetMapping("/usuarios")
    public ResponseEntity<List<UsuarioResponse>> listarUsuarios() {
        return ResponseEntity.ok(reporteService.listarUsuarios());
    }

    /** Consulta b: listado de clientes. */
    @GetMapping("/clientes")
    public ResponseEntity<List<ClienteResponse>> listarClientes() {
        return ResponseEntity.ok(reporteService.listarClientes());
    }

    /**
     * Consulta c: total con IVA acumulado por cliente. Solo clientes con al
     * menos una venta; lista vacía si aún no hay ventas.
     */
    @GetMapping("/ventas-por-cliente")
    public ResponseEntity<List<VentasPorClienteResponse>> ventasPorCliente() {
        return ResponseEntity.ok(reporteService.ventasPorCliente());
    }
}
