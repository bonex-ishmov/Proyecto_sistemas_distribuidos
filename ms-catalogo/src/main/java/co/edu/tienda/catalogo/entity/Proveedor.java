package co.edu.tienda.catalogo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "proveedores")
public class Proveedor {

    /**
     * El NIT es la clave primaria (no hay id autogenerado).
     * Asi lo define el modelo entidad-relacion del documento,
     * y asi lo referencia el CSV de productos por su columna nitproveedor.
     */
    @Id
    @Column(name = "nitproveedor")
    private Long nitproveedor;

    @Column(name = "nombre_proveedor", nullable = false, length = 100)
    private String nombreProveedor;

    @Column(name = "direccion_proveedor", nullable = false, length = 150)
    private String direccionProveedor;

    @Column(name = "telefono_proveedor", nullable = false, length = 20)
    private String telefonoProveedor;

    @Column(name = "ciudad_proveedor", nullable = false, length = 60)
    private String ciudadProveedor;

    public Proveedor() {
    }

    public Proveedor(Long nitproveedor, String nombreProveedor, String direccionProveedor,
                     String telefonoProveedor, String ciudadProveedor) {
        this.nitproveedor = nitproveedor;
        this.nombreProveedor = nombreProveedor;
        this.direccionProveedor = direccionProveedor;
        this.telefonoProveedor = telefonoProveedor;
        this.ciudadProveedor = ciudadProveedor;
    }

    public Long getNitproveedor() { return nitproveedor; }
    public void setNitproveedor(Long nitproveedor) { this.nitproveedor = nitproveedor; }

    public String getNombreProveedor() { return nombreProveedor; }
    public void setNombreProveedor(String nombreProveedor) { this.nombreProveedor = nombreProveedor; }

    public String getDireccionProveedor() { return direccionProveedor; }
    public void setDireccionProveedor(String direccionProveedor) { this.direccionProveedor = direccionProveedor; }

    public String getTelefonoProveedor() { return telefonoProveedor; }
    public void setTelefonoProveedor(String telefonoProveedor) { this.telefonoProveedor = telefonoProveedor; }

    public String getCiudadProveedor() { return ciudadProveedor; }
    public void setCiudadProveedor(String ciudadProveedor) { this.ciudadProveedor = ciudadProveedor; }
}
