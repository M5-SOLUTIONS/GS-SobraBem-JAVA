package br.com.sobrabem.repository;

import br.com.sobrabem.domain.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    boolean existsByReceptorId(Long receptorId);
}