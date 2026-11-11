// domain/Reserva.java
package br.com.sobrabem.domain.entity;

import br.com.sobrabem.domain.enums.StatusReserva;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "sb_t_reserva")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reserva")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_doacao")
    private Doacao doacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_receptor")
    private Usuario receptor;

    private Integer quantidade;

    @Enumerated(EnumType.STRING)
    private StatusReserva status;

    private String codigoRetirada;
    private Instant criadoEm;
    private Instant retiradoEm;

    public Reserva(Doacao doacao, Usuario receptor, int quantidade, String codigoRetirada) {
        this.doacao = doacao;
        this.receptor = receptor;
        this.quantidade = quantidade;
        this.codigoRetirada = codigoRetirada;
        this.status = StatusReserva.ATIVA;
    }

    @PrePersist
    void antesDeInserir() {
        if (criadoEm == null) criadoEm = Instant.now();
    }

    public boolean pertenceA(Long receptorId) {
        return receptor.getId().equals(receptorId);
    }

    public void cancelar() {
        status = StatusReserva.CANCELADA;
    }

    public void confirmarRetirada() {
        status = StatusReserva.RETIRADA;
        retiradoEm = Instant.now();
    }
}