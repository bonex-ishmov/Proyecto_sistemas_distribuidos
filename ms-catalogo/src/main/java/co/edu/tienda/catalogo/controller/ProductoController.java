package co.edu.tienda.catalogo.controller;

import co.edu.tienda.catalogo.dto.CargaResponse;
import co.edu.tienda.catalogo.dto.ProductoResponse;
import co.edu.tienda.catalogo.mapper.ProductoMapper;
import co.edu.tienda.catalogo.service.ProductoService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    /**
     * El archivo viaja como multipart/form-data, no como JSON.
     * required = false para poder responder nuestro propio mensaje
     * cuando el usuario no adjunta nada.
     *
     * En Postman: Body -> form-data, key "archivo" de tipo File.
     */
    @PostMapping(value = "/carga", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CargaResponse> cargar(
            @RequestParam(value = "archivo", required = false) MultipartFile archivo) {

        return ResponseEntity.ok(productoService.cargar(archivo));
    }

    @GetMapping
    public ResponseEntity<List<ProductoResponse>> listar() {
        return ResponseEntity.ok(ProductoMapper.aResponse(productoService.listar()));
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<ProductoResponse> consultar(@PathVariable Long codigo) {
        return ResponseEntity.ok(
                ProductoMapper.aResponse(productoService.buscarPorCodigo(codigo)));
    }

    /** Uso interno: ms-ventas pide varios productos de una sola vez. */
    @PostMapping("/lote")
    public ResponseEntity<List<ProductoResponse>> consultarLote(
            @RequestBody List<Long> codigos) {

        return ResponseEntity.ok(
                ProductoMapper.aResponse(productoService.buscarPorCodigos(codigos)));
    }
}
