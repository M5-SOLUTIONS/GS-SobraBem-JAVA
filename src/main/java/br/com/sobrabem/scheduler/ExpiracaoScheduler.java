package br.com.sobrabem.scheduler;

import br.com.sobrabem.repository.DoacaoRepository;
import br.com.sobrabem.repository.ReservaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class ExpiracaoScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExpiracaoScheduler.class);

    private final DoacaoRepository doacoes;
    private final ReservaRepository reservas;

    public ExpiracaoScheduler(DoacaoRepository doacoes, ReservaRepository reservas) {
        this.doacoes = doacoes;
        this.reservas = reservas;
    }

    @Scheduled(fixedRate = 300_000)
    @Transactional
    public void expirar() {
        // o mesmo instante nos dois UPDATEs, para não haver uma doação que vence "entre" eles
        Instant agora = Instant.now();

        int reservasExpiradas = reservas.expirarDeDoacoesVencidas(agora);
        int doacoesExpiradas = doacoes.expirarVencidas(agora);
        if (reservasExpiradas + doacoesExpiradas > 0) {
            log.info("Expiração: {} doação(ões) e {} reserva(s)", doacoesExpiradas, reservasExpiradas);
        }
    }
}