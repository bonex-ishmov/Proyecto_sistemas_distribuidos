package co.edu.tienda.catalogo.service;

import co.edu.tienda.catalogo.dto.CargaResponse;
import co.edu.tienda.catalogo.entity.Proveedor;
import co.edu.tienda.catalogo.exception.ArchivoInvalidoException;
import co.edu.tienda.catalogo.exception.DatosInvalidosException;
import co.edu.tienda.catalogo.repository.ProductoRepository;
import co.edu.tienda.catalogo.repository.ProveedorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class ProductoServiceTest {

    private static final String CABECERA =
            "codigo_producto,nombre_producto,nitproveedor,precio_compra,ivacompra,precio_venta\n";

    private ProductoRepository productoRepositorio;
    private ProveedorRepository proveedorRepositorio;
    private ProductoService productoService;

    @BeforeEach
    void preparar() {
        productoRepositorio = mock(ProductoRepository.class);
        proveedorRepositorio = mock(ProveedorRepository.class);
        productoService = new ProductoService(productoRepositorio, proveedorRepositorio);

        when(proveedorRepositorio.findByNitproveedorIn(anyList()))
                .thenReturn(List.of(proveedor(1L), proveedor(2L)));
        when(productoRepositorio.saveAll(anyList()))
                .thenAnswer(inv -> inv.getArgument(0));
        when(productoRepositorio.count()).thenReturn(0L);
    }

    private Proveedor proveedor(Long nit) {
        return new Proveedor(nit, "Proveedor " + nit, "Calle 1", "6011111111", "Bogota");
    }

    private MultipartFile archivo(String contenido) {
        return new MockMultipartFile("archivo", "productos.csv", "text/csv",
                contenido.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Carga exitosa de un archivo con cabecera")
    void cargaExitosa() {
        CargaResponse respuesta = productoService.cargar(archivo(
                CABECERA + "1,Melocotones,1,25505,19,30351\n2,Manzanas,2,18108,19,21549\n"));

        assertEquals("Archivo Cargado Exitosamente", respuesta.mensaje());
        assertEquals(2, respuesta.registrosCargados());
        verify(productoRepositorio).deleteAllInBatch();
    }

    @Test
    @DisplayName("El archivo sin cabecera tambien se carga")
    void cargaSinCabecera() {
        CargaResponse respuesta = productoService.cargar(
                archivo("1,Melocotones,1,25505,19,30351\n"));

        assertEquals(1, respuesta.registrosCargados());
    }

    @Test
    @DisplayName("Archivo ausente o vacio lanza ArchivoInvalidoException")
    void archivoVacio() {
        assertThrows(ArchivoInvalidoException.class, () -> productoService.cargar(null));
        assertThrows(ArchivoInvalidoException.class, () -> productoService.cargar(archivo("")));
    }

    @Test
    @DisplayName("Extension distinta de .csv lanza ArchivoInvalidoException")
    void extensionInvalida() {
        MultipartFile txt = new MockMultipartFile("archivo", "productos.txt",
                "text/plain", "1,x,1,1,1,1".getBytes(StandardCharsets.UTF_8));

        assertThrows(ArchivoInvalidoException.class, () -> productoService.cargar(txt));
    }

    @Test
    @DisplayName("RF-08: un proveedor inexistente aborta toda la carga")
    void proveedorInexistente() {
        DatosInvalidosException ex = assertThrows(DatosInvalidosException.class,
                () -> productoService.cargar(archivo(
                        CABECERA + "1,Melocotones,999,25505,19,30351\n")));

        assertEquals("Error: datos leidos invalidos", ex.getMessage());
        assertTrue(ex.getErrores().get(0).contains("999"));
        verify(productoRepositorio, never()).deleteAllInBatch();
    }

    @Test
    @DisplayName("Un precio no numerico aborta la carga sin borrar nada")
    void precioInvalido() {
        assertThrows(DatosInvalidosException.class, () -> productoService.cargar(archivo(
                CABECERA + "1,Melocotones,1,veinte mil,19,30351\n")));

        verify(productoRepositorio, never()).deleteAllInBatch();
        verify(productoRepositorio, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Un codigo duplicado dentro del archivo se detecta")
    void codigoDuplicado() {
        DatosInvalidosException ex = assertThrows(DatosInvalidosException.class,
                () -> productoService.cargar(archivo(CABECERA
                        + "1,Melocotones,1,25505,19,30351\n"
                        + "1,Manzanas,2,18108,19,21549\n")));

        assertTrue(ex.getErrores().get(0).contains("duplicado"));
    }

    @Test
    @DisplayName("Se acumulan todos los errores, no solo el primero")
    void erroresAcumulados() {
        DatosInvalidosException ex = assertThrows(DatosInvalidosException.class,
                () -> productoService.cargar(archivo(CABECERA
                        + "abc,Melocotones,1,25505,19,30351\n"
                        + "2,,1,18108,19,21549\n"
                        + "3,Platanos,1,-100,19,35320\n")));

        assertEquals(3, ex.getErrores().size());
    }

    @Test
    @DisplayName("El IVA fuera del rango 0-100 se rechaza")
    void ivaFueraDeRango() {
        assertThrows(DatosInvalidosException.class, () -> productoService.cargar(archivo(
                CABECERA + "1,Melocotones,1,25505,150,30351\n")));
    }

    @Test
    @DisplayName("Una fila con numero de columnas incorrecto se reporta con su linea")
    void columnasIncorrectas() {
        DatosInvalidosException ex = assertThrows(DatosInvalidosException.class,
                () -> productoService.cargar(archivo(CABECERA
                        + "1,Melocotones,1,25505,19,30351\n"
                        + "2,Manzanas,1,18108\n")));

        assertTrue(ex.getErrores().get(0).contains("Linea 3"));
    }

    @Test
    @DisplayName("El nombre que supera 50 caracteres se rechaza")
    void nombreMuyLargo() {
        String nombreLargo = "P".repeat(60);
        assertThrows(DatosInvalidosException.class, () -> productoService.cargar(archivo(
                CABECERA + "1," + nombreLargo + ",1,25505,19,30351\n")));
    }

    @Test
    @DisplayName("La lista vacia en consulta por lote no golpea la base de datos")
    void loteVacio() {
        assertTrue(productoService.buscarPorCodigos(List.of()).isEmpty());
        assertTrue(productoService.buscarPorCodigos(null).isEmpty());
        verify(productoRepositorio, never()).findByCodigoProductoIn(any());
    }
}
