package br.com.sobrabem.repository;

import br.com.sobrabem.domain.entity.Usuario;
import br.com.sobrabem.domain.enums.Papel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPapel(Papel papel);
}