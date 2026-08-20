package co.edu.tienda.identidad.repository;

import co.edu.tienda.identidad.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Spring Data genera la consulta a partir del nombre del método:
     * SELECT * FROM usuarios WHERE usuario = ?
     */
    Optional<Usuario> findByUsuario(String usuario);

    boolean existsByUsuario(String usuario);

    boolean existsByEmailUsuario(String emailUsuario);
}