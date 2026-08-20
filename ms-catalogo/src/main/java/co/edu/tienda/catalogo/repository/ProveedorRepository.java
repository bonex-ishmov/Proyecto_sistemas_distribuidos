package co.edu.tienda.catalogo.repository;

import co.edu.tienda.catalogo.entity.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    /**
     * Devuelve, de una lista de NITs, cuales existen realmente.
     * Permite validar TODO el CSV con UNA sola consulta,
     * en lugar de una por cada fila del archivo.
     */
    List<Proveedor> findByNitproveedorIn(List<Long> nits);
}
