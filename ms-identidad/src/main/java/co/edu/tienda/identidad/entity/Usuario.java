package co.edu.tienda.identidad.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @Column(name = "cedula_usuario")
    private Long cedulaUsuario;

    @Column(name = "nombre_usuario", nullable = false, length = 100)
    private String nombreUsuario;

    @Column(name = "email_usuario", nullable = false, unique = true, length = 100)
    private String emailUsuario;

    @Column(name = "usuario", nullable = false, unique = true, length = 50)
    private String usuario;

    /** Hash BCrypt. NUNCA la contraseña en texto plano. */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    public Usuario() {
        // Constructor vacío obligatorio para JPA
    }

    public Usuario(Long cedulaUsuario, String nombreUsuario, String emailUsuario,
                   String usuario, String password, Boolean activo) {
        this.cedulaUsuario = cedulaUsuario;
        this.nombreUsuario = nombreUsuario;
        this.emailUsuario = emailUsuario;
        this.usuario = usuario;
        this.password = password;
        this.activo = activo;
    }

    public Long getCedulaUsuario() { return cedulaUsuario; }
    public void setCedulaUsuario(Long cedulaUsuario) { this.cedulaUsuario = cedulaUsuario; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public String getEmailUsuario() { return emailUsuario; }
    public void setEmailUsuario(String emailUsuario) { this.emailUsuario = emailUsuario; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}