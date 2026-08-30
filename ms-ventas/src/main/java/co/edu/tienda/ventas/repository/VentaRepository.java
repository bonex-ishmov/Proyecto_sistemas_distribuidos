package co.edu.tienda.ventas.repository;

import co.edu.tienda.ventas.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VentaRepository extends JpaRepository<Venta, Long> {
    // Las consultas (buscar por codigo, total por cliente) llegan en el Incremento 3.
}