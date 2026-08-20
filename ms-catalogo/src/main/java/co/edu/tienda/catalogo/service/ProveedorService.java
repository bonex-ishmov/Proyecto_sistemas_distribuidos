package co.edu.tienda.catalogo.service;

import co.edu.tienda.catalogo.dto.ProveedorRequest;
import co.edu.tienda.catalogo.dto.ProveedorUpdateRequest;
import co.edu.tienda.catalogo.entity.Proveedor;
import co.edu.tienda.catalogo.exception.RecursoNoEncontradoException;
import co.edu.tienda.catalogo.exception.ReglaNegocioException;
import co.edu.tienda.catalogo.repository.ProductoRepository;
import co.edu.tienda.catalogo.repository.ProveedorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;

    public ProveedorService(ProveedorRepository proveedorRepository,
                            ProductoRepository productoRepository) {
        this.proveedorRepository = proveedorRepository;
        this.productoRepository = productoRepository;
    }

    @Transactional(readOnly = true)
    public List<Proveedor> listar() {
        return proveedorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Proveedor buscarPorNit(Long nit) {
        return proveedorRepository.findById(nit)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proveedor Inexistente"));
    }

    @Transactional
    public Proveedor crear(ProveedorRequest request) {

        if (proveedorRepository.existsById(request.nitproveedor())) {
            throw new ReglaNegocioException(
                    "Ya existe un proveedor con el NIT " + request.nitproveedor());
        }

        Proveedor nuevo = new Proveedor(
                request.nitproveedor(),
                request.nombreProveedor(),
                request.direccionProveedor(),
                request.telefonoProveedor(),
                request.ciudadProveedor()
        );
        return proveedorRepository.save(nuevo);
    }

    @Transactional
    public Proveedor actualizar(Long nit, ProveedorUpdateRequest request) {

        Proveedor proveedor = buscarPorNit(nit);

        proveedor.setNombreProveedor(request.nombreProveedor());
        proveedor.setDireccionProveedor(request.direccionProveedor());
        proveedor.setTelefonoProveedor(request.telefonoProveedor());
        proveedor.setCiudadProveedor(request.ciudadProveedor());

        return proveedorRepository.save(proveedor);
    }

    @Transactional
    public void eliminar(Long nit) {
        Proveedor proveedor = buscarPorNit(nit);

        // Integridad referencial DENTRO del microservicio.
        if (productoRepository.existsByProveedorNitproveedor(nit)) {
            throw new ReglaNegocioException(
                    "No se puede eliminar el proveedor: tiene productos asociados");
        }

        proveedorRepository.delete(proveedor);
    }
}
