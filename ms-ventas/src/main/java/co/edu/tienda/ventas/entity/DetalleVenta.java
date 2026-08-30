package co.edu.tienda.ventas.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "detalle_ventas", indexes = {
        @Index(name = "idx_detalle_ventas_codigo_venta", columnList = "codigo_venta")
})
public class DetalleVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Long idDetalle;

    /**
     * FK real (fk_detalle_venta): la venta y su detalle viven en el mismo
     * esquema db_ventas, asi que aqui SI usamos integridad referencial de MySQL.
     * Entre microservicios distintos nunca lo hariamos.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codigo_venta", nullable = false,
                foreignKey = @ForeignKey(name = "fk_detalle_venta"))
    private Venta venta;

    /** Sin FK: el producto vive en ms-catalogo (otro esquema y servicio). */
    @Column(name = "codigo_producto", nullable = false)
    private Long codigoProducto;

    /**
     * Snapshot deliberado. POST /api/productos/carga de ms-catalogo borra la
     * tabla productos entera; congelamos el nombre para que la venta siga
     * siendo legible aunque el producto ya no exista en el catalogo.
     */
    @Column(name = "nombre_producto", nullable = false, length = 50)
    private String nombreProducto;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    /** precio_venta del producto congelado al momento de la venta. */
    @Column(name = "valor_unitario", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorUnitario;

    /** Porcentaje (ej. 19.00), no un monto. Copiado de producto.ivacompra. */
    @Column(name = "iva_porcentaje", nullable = false, precision = 5, scale = 2)
    private BigDecimal ivaPorcentaje;

    /** IVA de esta linea: valor_unitario * cantidad * iva_porcentaje / 100. */
    @Column(name = "valor_iva", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorIva;

    /** Base SIN IVA de la linea: cantidad * valor_unitario. */
    @Column(name = "valor_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorTotal;

    public DetalleVenta() {
    }

    public Long getIdDetalle() { return idDetalle; }
    public void setIdDetalle(Long idDetalle) { this.idDetalle = idDetalle; }

    public Venta getVenta() { return venta; }
    public void setVenta(Venta venta) { this.venta = venta; }

    public Long getCodigoProducto() { return codigoProducto; }
    public void setCodigoProducto(Long codigoProducto) { this.codigoProducto = codigoProducto; }

    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public BigDecimal getValorUnitario() { return valorUnitario; }
    public void setValorUnitario(BigDecimal valorUnitario) { this.valorUnitario = valorUnitario; }

    public BigDecimal getIvaPorcentaje() { return ivaPorcentaje; }
    public void setIvaPorcentaje(BigDecimal ivaPorcentaje) { this.ivaPorcentaje = ivaPorcentaje; }

    public BigDecimal getValorIva() { return valorIva; }
    public void setValorIva(BigDecimal valorIva) { this.valorIva = valorIva; }

    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }
}