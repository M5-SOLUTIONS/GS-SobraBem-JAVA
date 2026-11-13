package br.com.sobrabem.repository;

import br.com.sobrabem.domain.entity.Reserva;
import br.com.sobrabem.domain.enums.StatusReserva;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    boolean existsByDoacaoIdAndReceptorIdAndStatus(Long doacaoId, Long receptorId, StatusReserva status);

    boolean existsByDoacaoId(Long doacaoId);

    boolean existsByReceptorId(Long receptorId);

    List<Reserva> findByDoacaoIdAndStatus(Long doacaoId, StatusReserva status);

    /** Só o id da doação, sem carregar a reserva: ela é lida depois de a doação ser travada. */
    @Query("select r.doacao.id from Reserva r where r.id = :id")
    Optional<Long> buscarIdDaDoacao(@Param("id") Long id);

    @EntityGraph(attributePaths = {"doacao", "doacao.doador", "receptor"})
    @Query("select r from Reserva r where r.doacao.id = :doacaoId and (:status is null or r.status = :status)")
    Page<Reserva> buscarDaDoacao(@Param("doacaoId") Long doacaoId,
                                 @Param("status") StatusReserva status,
                                 Pageable pageable);

    @EntityGraph(attributePaths = {"doacao", "doacao.doador", "receptor"})
    @Query("select r from Reserva r where r.receptor.id = :receptorId and (:status is null or r.status = :status)")
    Page<Reserva> buscarDoReceptor(@Param("receptorId") Long receptorId,
                                   @Param("status") StatusReserva status,
                                   Pageable pageable);

    @Modifying
    @Query("""
            update Reserva r set r.status = br.com.sobrabem.domain.enums.StatusReserva.EXPIRADA
             where r.status = br.com.sobrabem.domain.enums.StatusReserva.ATIVA
               and r.doacao.id in (
                   select d.id from Doacao d
                    where d.status in (br.com.sobrabem.domain.enums.StatusDoacao.ATIVA,
                                       br.com.sobrabem.domain.enums.StatusDoacao.ESGOTADA)
                      and d.prazoRetirada <= :agora)
            """)
    int expirarDeDoacoesVencidas(@Param("agora") Instant agora);
}