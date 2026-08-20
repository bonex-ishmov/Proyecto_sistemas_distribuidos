package co.edu.tienda.identidad.controller;

import co.edu.tienda.identidad.dto.EstadoRequest;
import co.edu.tienda.identidad.dto.UsuarioRequest;
import co.edu.tienda.identidad.dto.UsuarioResponse;
import co.edu.tienda.identidad.dto.UsuarioUpdateRequest;
import co.edu.tienda.identidad.mapper.UsuarioMapper;
import co.edu.tienda.identidad.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(UsuarioMapper.aResponse(usuarioService.listar()));
    }

    @GetMapping("/{cedula}")
    public ResponseEntity<UsuarioResponse> consultar(@PathVariable Long cedula) {
        return ResponseEntity.ok(
                UsuarioMapper.aResponse(usuarioService.buscarPorCedula(cedula)));
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioRequest request) {
        UsuarioResponse creado = UsuarioMapper.aResponse(usuarioService.crear(request));
        return ResponseEntity
                .created(URI.create("/api/usuarios/" + creado.cedulaUsuario()))
                .body(creado);
    }

    @PutMapping("/{cedula}")
    public ResponseEntity<UsuarioResponse> actualizar(
            @PathVariable Long cedula,
            @Valid @RequestBody UsuarioUpdateRequest request) {

        return ResponseEntity.ok(
                UsuarioMapper.aResponse(usuarioService.actualizar(cedula, request)));
    }

    @DeleteMapping("/{cedula}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long cedula) {
        usuarioService.eliminar(cedula);
    }

    @PatchMapping("/{cedula}/estado")
    public ResponseEntity<UsuarioResponse> cambiarEstado(
            @PathVariable Long cedula,
            @Valid @RequestBody EstadoRequest request) {

        return ResponseEntity.ok(
                UsuarioMapper.aResponse(usuarioService.cambiarEstado(cedula, request.activo())));
    }
}