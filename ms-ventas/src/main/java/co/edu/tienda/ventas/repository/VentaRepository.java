package co.edu.tienda.ventas.repository;

import co.edu.tienda.ventas.dto.ResumenPeriodoResponse;
import co.edu.tienda.ventas.dto.TotalPorClienteResponse;
import co.edu.tienda.ventas.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    /**
     * Resumen de un periodo para el dashboard de inicio (GET /api/ventas/resumen):
     * numero de ventas y suma de total_con_iva con fecha_venta en [desde, hasta).
     * La agregacion la hace la BD; el rango [desde, hasta) lo calcula VentaService
     * en la zona horaria de la tienda. SUM devuelve null si el periodo no tiene
     * ventas: VentaService lo normaliza a 0.00.
     */
    @Query("""
            SELECT new co.edu.tienda.ventas.dto.ResumenPeriodoResponse(
                       COUNT(v), SUM(v.totalConIva))
            FROM Venta v
            WHERE v.fechaVenta >= :desde AND v.fechaVenta < :hasta
            """)
    ResumenPeriodoResponse resumenEntre(@Param("desde") LocalDateTime desde,
                                        @Param("hasta") LocalDateTime hasta);
}
