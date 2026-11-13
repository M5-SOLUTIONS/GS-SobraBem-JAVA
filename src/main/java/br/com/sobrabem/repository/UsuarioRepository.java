package br.com.sobrabem.repository;

import br.com.sobrabem.domain.entity.Usuario;
import br.com.sobrabem.domain.enums.Papel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPapel(Papel papel);

    /** papel null = todos. Uma query só, em vez de um método com e outro sem filtro. */
    @Query("select u from Usuario u where (:papel is null or u.papel = :papel)")
    Page<Usuario> buscar(@Param("papel") Papel papel, Pageable pageable);
}