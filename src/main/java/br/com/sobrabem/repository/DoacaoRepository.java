package br.com.sobrabem.repository;

import br.com.sobrabem.domain.entity.Doacao;
import br.com.sobrabem.domain.enums.StatusDoacao;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface DoacaoRepository extends JpaRepository<Doacao, Long> {

    /** Vitrine do receptor: só ATIVA com prazo no futuro. Filtros opcionais (null = sem filtro). */
    @EntityGraph(attributePaths = {"categoria", "doador"})
    @Query("""
            select d from Doacao d
             where d.status = br.com.sobrabem.domain.enums.StatusDoacao.ATIVA
               and d.prazoRetirada > :agora
               and (:categoriaId is null or d.categoria.id = :categoriaId)
               and (:cidade is null or lower(d.doador.cidade) = lower(:cidade))
            """)
    Page<Doacao> buscarVitrine(@Param("agora") Instant agora,
                               @Param("categoriaId") Long categoriaId,
                               @Param("cidade") String cidade,
                               Pageable pageable);

    @EntityGraph(attributePaths = {"categoria", "doador"})
    @Query("select d from Doacao d where d.doador.id = :doadorId and (:status is null or d.status = :status)")
    Page<Doacao> buscarDoDoador(@Param("doadorId") Long doadorId,
                                @Param("status") StatusDoacao status,
                                Pageable pageable);

    /** SELECT ... FOR UPDATE: quem edita ou cancela trava a linha até o fim da transação. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Doacao d where d.id = :id")
    Optional<Doacao> buscarParaAtualizar(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Doacao d
               set d.quantidadeDisponivel = d.quantidadeDisponivel - :qtd,
                   d.status = case when d.quantidadeDisponivel - :qtd = 0
                                   then br.com.sobrabem.domain.enums.StatusDoacao.ESGOTADA
                                   else d.status end
             where d.id = :id
               and d.status = br.com.sobrabem.domain.enums.StatusDoacao.ATIVA
               and d.prazoRetirada > :agora
               and d.quantidadeDisponivel >= :qtd
            """)
    int descontarSaldo(@Param("id") Long id, @Param("qtd") int quantidade, @Param("agora") Instant agora);

    @Modifying
    @Query("""
            update Doacao d set d.status = br.com.sobrabem.domain.enums.StatusDoacao.EXPIRADA
             where d.status in (br.com.sobrabem.domain.enums.StatusDoacao.ATIVA,
                                br.com.sobrabem.domain.enums.StatusDoacao.ESGOTADA)
               and d.prazoRetirada <= :agora
            """)
    int expirarVencidas(@Param("agora") Instant agora);

    boolean existsByCategoriaId(Long categoriaId);

    boolean existsByDoadorId(Long doadorId);
}