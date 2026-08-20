package co.edu.tienda.catalogo.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "productos", indexes = {
        @Index(name = "idx_producto_nitproveedor", columnList = "nitproveedor")
})
public class Producto {

    /** El codigo viene en el archivo CSV: no es autogenerado. */
    @Id
    @Column(name = "codigo_producto")
    private Long codigoProducto;

    @Column(name = "nombre_producto", nullable = false, length = 50)
    private String nombreProducto;

    /**
     * CLAVE FORANEA REAL. Producto y Proveedor viven en el mismo esquema,
     * asi que aqui si usamos integridad referencial de MySQL.
     * Entre microservicios distintos nunca lo hariamos.
     *
     * EAGER a proposito: el catalogo es pequeno y asi evitamos
     * LazyInitializationException al mapear fuera de la transaccion.
     */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "nitproveedor", nullable = false,
                foreignKey = @ForeignKey(name = "fk_producto_proveedor"))
    private Proveedor proveedor;

    @Column(name = "precio_compra", nullable = false, precision = 15, scale = 2)
    private BigDecimal precioCompra;

    /** Porcentaje de IVA (ej. 19.00), no un valor monetario. */
    @Column(name = "ivacompra", nullable = false, precision = 5, scale = 2)
    private BigDecimal ivacompra;

    @Column(name = "precio_venta", nullable = false, precision = 15, scale = 2)
    private BigDecimal precioVenta;

    public Producto() {
    }

    public Producto(Long codigoProducto, String nombreProducto, Proveedor proveedor,
                    BigDecimal precioCompra, BigDecimal ivacompra, BigDecimal precioVenta) {
        this.codigoProducto = codigoProducto;
        this.nombreProducto = nombreProducto;
        this.proveedor = proveedor;
        this.precioCompra = precioCompra;
        this.ivacompra = ivacompra;
        this.precioVenta = precioVenta;
    }

    public Long getCodigoProducto() { return codigoProducto; }
    public void setCodigoProducto(Long codigoProducto) { this.codigoProducto = codigoProducto; }

    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }

    public Proveedor getProveedor() { return proveedor; }
    public void setProveedor(Proveedor proveedor) { this.proveedor = proveedor; }

    public BigDecimal getPrecioCompra() { return precioCompra; }
    public void setPrecioCompra(BigDecimal precioCompra) { this.precioCompra = precioCompra; }

    public BigDecimal getIvacompra() { return ivacompra; }
    public void setIvacompra(BigDecimal ivacompra) { this.ivacompra = ivacompra; }

    public BigDecimal getPrecioVenta() { return precioVenta; }
    public void setPrecioVenta(BigDecimal precioVenta) { this.precioVenta = precioVenta; }
}
