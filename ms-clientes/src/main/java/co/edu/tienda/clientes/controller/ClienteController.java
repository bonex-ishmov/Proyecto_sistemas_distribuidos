package co.edu.tienda.clientes.controller;

import co.edu.tienda.clientes.dto.ClienteRequest;
import co.edu.tienda.clientes.dto.ClienteResponse;
import co.edu.tienda.clientes.dto.ClienteUpdateRequest;
import co.edu.tienda.clientes.mapper.ClienteMapper;
import co.edu.tienda.clientes.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public ResponseEntity<List<ClienteResponse>> listar() {
        return ResponseEntity.ok(ClienteMapper.aResponse(clienteService.listar()));
    }

    @GetMapping("/{cedula}")
    public ResponseEntity<ClienteResponse> consultar(@PathVariable Long cedula) {
        return ResponseEntity.ok(
                ClienteMapper.aResponse(clienteService.buscarPorCedula(cedula)));
    }

    @PostMapping
    public ResponseEntity<ClienteResponse> crear(@Valid @RequestBody ClienteRequest request) {
        ClienteResponse creado = ClienteMapper.aResponse(clienteService.crear(request));
        return ResponseEntity
                .created(URI.create("/api/clientes/" + creado.cedulaCliente()))
                .body(creado);
    }

    @PutMapping("/{cedula}")
    public ResponseEntity<ClienteResponse> actualizar(
            @PathVariable Long cedula,
            @Valid @RequestBody ClienteUpdateRequest request) {

        return ResponseEntity.ok(
                ClienteMapper.aResponse(clienteService.actualizar(cedula, request)));
    }

    @DeleteMapping("/{cedula}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long cedula) {
        clienteService.eliminar(cedula);
    }

    /**
     * Endpoint de uso INTERNO entre microservicios.
     * ms-reportes envía una lista de cédulas y recibe los datos de una sola vez,
     * en lugar de hacer una petición HTTP por cada cliente.
     */
    @PostMapping("/lote")
    public ResponseEntity<List<ClienteResponse>> consultarLote(
            @RequestBody List<Long> cedulas) {

        return ResponseEntity.ok(
                ClienteMapper.aResponse(clienteService.buscarPorCedulas(cedulas)));
    }
}