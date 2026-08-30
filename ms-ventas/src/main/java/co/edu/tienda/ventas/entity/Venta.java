package co.edu.tienda.ventas.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ventas", indexes = {
        @Index(name = "idx_ventas_cedula_cliente", columnList = "cedula_cliente")
})
public class Venta {

    /**
     * Unica PK autogenerada del proyecto. El PDF (modulo 6) pide un
     * "codigo de venta - valor consecutivo"; lo delegamos al AUTO_INCREMENT
     * de MySQL. El resto de tablas usa clave natural (cedula, NIT, codigo).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codigo_venta")
    private Long codigoVenta;

    /** Sin FK: el cliente vive en ms-clientes. Se valida por HTTP al registrar. */
    @Column(name = "cedula_cliente", nullable = false)
    private Long cedulaCliente;

    /** Sin FK: el usuario vive en ms-identidad. Sale del JWT, nunca del body. */
    @Column(name = "cedula_usuario", nullable = false)
    private Long cedulaUsuario;

    /** Base gravable: suma de los valorTotal de linea, SIN IVA. */
    @Column(name = "total_venta", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalVenta;

    @Column(name = "valor_iva", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorIva;

    @Column(name = "total_con_iva", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalConIva;

    @Column(name = "fecha_venta", nullable = false)
    private LocalDateTime fechaVenta;

    /**
     * EAGER a proposito, mismo criterio que Producto->Proveedor en ms-catalogo:
     * el detalle de una venta es una lista corta y asi evitamos
     * LazyInitializationException al mapear a DTO. No hay endpoint que liste
     * TODAS las ventas, asi que no hay riesgo de N+1 masivo.
     */
    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.EAGER)
    private List<DetalleVenta> detalles = new ArrayList<>();

    public Venta() {
    }

    /** Mantiene los dos lados de la relacion en sincronia. */
    public void agregarDetalle(DetalleVenta detalle) {
        detalle.setVenta(this);
        this.detalles.add(detalle);
    }

    public Long getCodigoVenta() { return codigoVenta; }
    public void setCodigoVenta(Long codigoVenta) { this.codigoVenta = codigoVenta; }

    public Long getCedulaCliente() { return cedulaCliente; }
    public void setCedulaCliente(Long cedulaCliente) { this.cedulaCliente = cedulaCliente; }

    public Long getCedulaUsuario() { return cedulaUsuario; }
    public void setCedulaUsuario(Long cedulaUsuario) { this.cedulaUsuario = cedulaUsuario; }

    public BigDecimal getTotalVenta() { return totalVenta; }
    public void setTotalVenta(BigDecimal totalVenta) { this.totalVenta = totalVenta; }

    public BigDecimal getValorIva() { return valorIva; }
    public void setValorIva(BigDecimal valorIva) { this.valorIva = valorIva; }

    public BigDecimal getTotalConIva() { return totalConIva; }
    public void setTotalConIva(BigDecimal totalConIva) { this.totalConIva = totalConIva; }

    public LocalDateTime getFechaVenta() { return fechaVenta; }
    public void setFechaVenta(LocalDateTime fechaVenta) { this.fechaVenta = fechaVenta; }

    public List<DetalleVenta> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleVenta> detalles) { this.detalles = detalles; }
}