package br.com.sobrabem.repository;

import br.com.sobrabem.domain.entity.Doacao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoacaoRepository extends JpaRepository<Doacao, Long> {

    boolean existsByCategoriaId(Long categoriaId);

    boolean existsByDoadorId(Long doadorId);
}