package br.com.sobrabem.repository;

import br.com.sobrabem.domain.entity.Reserva;
import br.com.sobrabem.domain.enums.StatusReserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    boolean existsByReceptorId(Long receptorId);

    boolean existsByDoacaoId(Long doacaoId);

    List<Reserva> findByDoacaoIdAndStatus(Long doacaoId, StatusReserva status);
}