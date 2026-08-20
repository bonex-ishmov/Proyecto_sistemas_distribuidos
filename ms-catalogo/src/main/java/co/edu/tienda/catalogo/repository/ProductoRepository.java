package co.edu.tienda.catalogo.repository;

import co.edu.tienda.catalogo.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    /** Usado por ms-ventas: trae varios productos en una sola consulta. */
    List<Producto> findByCodigoProductoIn(List<Long> codigos);

    /** Usado por ProveedorService para bloquear el borrado. */
    boolean existsByProveedorNitproveedor(Long nitproveedor);
}
