package co.edu.tienda.clientes.repository;

import co.edu.tienda.clientes.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    /** Consulta por lote: la usará ms-reportes para no hacer N llamadas HTTP. */
    List<Cliente> findByCedulaClienteIn(List<Long> cedulas);
}