package co.edu.tienda.catalogo.controller;

import co.edu.tienda.catalogo.dto.ProveedorRequest;
import co.edu.tienda.catalogo.dto.ProveedorResponse;
import co.edu.tienda.catalogo.dto.ProveedorUpdateRequest;
import co.edu.tienda.catalogo.mapper.ProveedorMapper;
import co.edu.tienda.catalogo.service.ProveedorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/proveedores")
public class ProveedorController {

    private final ProveedorService proveedorService;

    public ProveedorController(ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    @GetMapping
    public ResponseEntity<List<ProveedorResponse>> listar() {
        return ResponseEntity.ok(ProveedorMapper.aResponse(proveedorService.listar()));
    }

    @GetMapping("/{nit}")
    public ResponseEntity<ProveedorResponse> consultar(@PathVariable Long nit) {
        return ResponseEntity.ok(
                ProveedorMapper.aResponse(proveedorService.buscarPorNit(nit)));
    }

    @PostMapping
    public ResponseEntity<ProveedorResponse> crear(@Valid @RequestBody ProveedorRequest request) {
        ProveedorResponse creado = ProveedorMapper.aResponse(proveedorService.crear(request));
        return ResponseEntity
                .created(URI.create("/api/proveedores/" + creado.nitproveedor()))
                .body(creado);
    }

    @PutMapping("/{nit}")
    public ResponseEntity<ProveedorResponse> actualizar(
            @PathVariable Long nit,
            @Valid @RequestBody ProveedorUpdateRequest request) {

        return ResponseEntity.ok(
                ProveedorMapper.aResponse(proveedorService.actualizar(nit, request)));
    }

    @DeleteMapping("/{nit}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long nit) {
        proveedorService.eliminar(nit);
    }
}
