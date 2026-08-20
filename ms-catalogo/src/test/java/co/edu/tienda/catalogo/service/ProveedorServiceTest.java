package co.edu.tienda.catalogo.service;

import co.edu.tienda.catalogo.dto.ProveedorRequest;
import co.edu.tienda.catalogo.dto.ProveedorUpdateRequest;
import co.edu.tienda.catalogo.entity.Proveedor;
import co.edu.tienda.catalogo.exception.RecursoNoEncontradoException;
import co.edu.tienda.catalogo.exception.ReglaNegocioException;
import co.edu.tienda.catalogo.repository.ProductoRepository;
import co.edu.tienda.catalogo.repository.ProveedorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProveedorServiceTest {

    private ProveedorRepository proveedorRepositorio;
    private ProductoRepository productoRepositorio;
    private ProveedorService proveedorService;

    @BeforeEach
    void preparar() {
        proveedorRepositorio = mock(ProveedorRepository.class);
        productoRepositorio = mock(ProductoRepository.class);
        proveedorService = new ProveedorService(proveedorRepositorio, productoRepositorio);
        when(proveedorRepositorio.save(any(Proveedor.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private ProveedorRequest peticionNueva() {
        return new ProveedorRequest(900123456L, "Distribuidora La Cosecha S.A.S.",
                "Carrera 68 # 45-12", "6014567890", "Bogota");
    }

    private Proveedor proveedorDemo() {
        return new Proveedor(900123456L, "Distribuidora La Cosecha S.A.S.",
                "Carrera 68 # 45-12", "6014567890", "Bogota");
    }

    @Test
    @DisplayName("Crear un proveedor nuevo lo guarda con todos sus datos")
    void crearCorrecto() {
        when(proveedorRepositorio.existsById(900123456L)).thenReturn(false);

        Proveedor creado = proveedorService.crear(peticionNueva());

        assertEquals(900123456L, creado.getNitproveedor());
        assertEquals("Bogota", creado.getCiudadProveedor());
        verify(proveedorRepositorio).save(any(Proveedor.class));
    }

    @Test
    @DisplayName("NIT duplicado lanza ReglaNegocioException y no guarda")
    void nitDuplicado() {
        when(proveedorRepositorio.existsById(900123456L)).thenReturn(true);

        assertThrows(ReglaNegocioException.class,
                () -> proveedorService.crear(peticionNueva()));
        verify(proveedorRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("Se acepta un NIT de un solo digito, como los del CSV del documento")
    void nitDeUnDigito() {
        when(proveedorRepositorio.existsById(1L)).thenReturn(false);

        Proveedor creado = proveedorService.crear(new ProveedorRequest(
                1L, "Proveedor Uno", "Calle 1", "6011111111", "Medellin"));

        assertEquals(1L, creado.getNitproveedor());
    }

    @Test
    @DisplayName("Consultar un NIT inexistente lanza 404")
    void consultaInexistente() {
        when(proveedorRepositorio.findById(999999999L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> proveedorService.buscarPorNit(999999999L));
    }

    @Test
    @DisplayName("Actualizar modifica los cuatro campos editables y conserva el NIT")
    void actualizarCorrecto() {
        when(proveedorRepositorio.findById(900123456L)).thenReturn(Optional.of(proveedorDemo()));

        Proveedor actualizado = proveedorService.actualizar(900123456L,
                new ProveedorUpdateRequest("La Cosecha Ltda.", "Avenida 30 # 8-20",
                        "6019998877", "Cali"));

        assertEquals("La Cosecha Ltda.", actualizado.getNombreProveedor());
        assertEquals("Avenida 30 # 8-20", actualizado.getDireccionProveedor());
        assertEquals("6019998877", actualizado.getTelefonoProveedor());
        assertEquals("Cali", actualizado.getCiudadProveedor());
        assertEquals(900123456L, actualizado.getNitproveedor());
    }

    @Test
    @DisplayName("Eliminar un proveedor inexistente lanza 404 y no borra")
    void eliminarInexistente() {
        when(proveedorRepositorio.findById(999999999L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> proveedorService.eliminar(999999999L));
        verify(proveedorRepositorio, never()).delete(any());
    }

    @Test
    @DisplayName("No se puede eliminar un proveedor que tiene productos asociados")
    void eliminarConProductos() {
        when(proveedorRepositorio.findById(900123456L)).thenReturn(Optional.of(proveedorDemo()));
        when(productoRepositorio.existsByProveedorNitproveedor(900123456L)).thenReturn(true);

        assertThrows(ReglaNegocioException.class,
                () -> proveedorService.eliminar(900123456L));
        verify(proveedorRepositorio, never()).delete(any());
    }

    @Test
    @DisplayName("Un proveedor sin productos si se puede eliminar")
    void eliminarSinProductos() {
        when(proveedorRepositorio.findById(900123456L)).thenReturn(Optional.of(proveedorDemo()));
        when(productoRepositorio.existsByProveedorNitproveedor(900123456L)).thenReturn(false);

        proveedorService.eliminar(900123456L);

        verify(proveedorRepositorio).delete(any(Proveedor.class));
    }
}
