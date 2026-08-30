package co.edu.tienda.ventas.repository;

import co.edu.tienda.ventas.dto.TotalPorClienteResponse;
import co.edu.tienda.ventas.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    /**
     * Reporte del modulo 7: una fila por cliente con al menos una venta,
     * con la suma de total_con_iva. Ordenado por cedula para salida estable.
     * La agregacion la hace la BD, no se traen las ventas a memoria.
     */
    @Query("""
            SELECT new co.edu.tienda.ventas.dto.TotalPorClienteResponse(
                       v.cedulaCliente, SUM(v.totalConIva))
            FROM Venta v
            GROUP BY v.cedulaCliente
            ORDER BY v.cedulaCliente
            """)
    List<TotalPorClienteResponse> totalConIvaPorCliente();
}
