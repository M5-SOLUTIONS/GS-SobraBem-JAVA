package br.com.sobrabem.scheduler;

import br.com.sobrabem.repository.DoacaoRepository;
import br.com.sobrabem.repository.ReservaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class ExpiracaoSchedulerTest {

    @Mock
    DoacaoRepository doacoes;
    @Mock
    ReservaRepository reservas;
    @InjectMocks
    ExpiracaoScheduler scheduler;

    @Test
    void expiraAsReservasAntesDasDoacoesComOMesmoInstante() {
        scheduler.expirar();

        ArgumentCaptor<Instant> instanteReservas = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> instanteDoacoes = ArgumentCaptor.forClass(Instant.class);
        InOrder ordem = inOrder(reservas, doacoes);
        ordem.verify(reservas).expirarDeDoacoesVencidas(instanteReservas.capture());
        ordem.verify(doacoes).expirarVencidas(instanteDoacoes.capture());
        assertThat(instanteReservas.getValue()).isEqualTo(instanteDoacoes.getValue());
    }
}