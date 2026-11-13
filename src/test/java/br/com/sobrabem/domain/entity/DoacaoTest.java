package br.com.sobrabem.domain.entity;

import br.com.sobrabem.domain.enums.Papel;
import br.com.sobrabem.domain.enums.StatusDoacao;
import br.com.sobrabem.domain.enums.Unidade;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static br.com.sobrabem.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

class DoacaoTest {

    private final Usuario doador = usuario(1L, Papel.DOADOR);
    private final Categoria paes = categoria(1L, 12);
    private final Instant prazo = Instant.now().plus(2, ChronoUnit.HOURS);

    @Test
    void novaDoacaoComecaAtivaComSaldoCheio() { // RN03
        Doacao d = doacao(10L, doador, paes, 30, prazo);

        assertThat(d.getStatus()).isEqualTo(StatusDoacao.ATIVA);
        assertThat(d.getQuantidadeDisponivel()).isEqualTo(30);
    }

    @Test
    void edicaoQueZeraOSaldoDeixaEsgotada() { // RN04
        Doacao d = doacao(10L, doador, paes, 10, prazo);
        definir(d, "quantidadeDisponivel", 4); // 6 reservadas

        d.atualizar(paes, "Pão", null, 6, Unidade.UNIDADE, prazo, null, false);

        assertThat(d.getQuantidadeDisponivel()).isZero();
        assertThat(d.getStatus()).isEqualTo(StatusDoacao.ESGOTADA);
    }

    @Test
    void edicaoQueAumentaOTotalReativaEsgotada() {
        Doacao d = doacao(10L, doador, paes, 10, prazo);
        definir(d, "quantidadeDisponivel", 0);
        definir(d, "status", StatusDoacao.ESGOTADA);

        d.atualizar(paes, "Pão", null, 15, Unidade.UNIDADE, prazo, null, false);

        assertThat(d.getQuantidadeDisponivel()).isEqualTo(5);
        assertThat(d.getStatus()).isEqualTo(StatusDoacao.ATIVA);
    }

    @Test
    void devolverSaldoReativaEsgotada() { // RN08
        Doacao d = doacao(10L, doador, paes, 10, prazo);
        definir(d, "quantidadeDisponivel", 0);
        definir(d, "status", StatusDoacao.ESGOTADA);

        d.devolverSaldo(4);

        assertThat(d.getQuantidadeDisponivel()).isEqualTo(4);
        assertThat(d.getStatus()).isEqualTo(StatusDoacao.ATIVA);
    }

    @Test
    void devolverSaldoNaoReativaCancelada() {
        Doacao d = doacao(10L, doador, paes, 10, prazo);
        d.cancelar();

        d.devolverSaldo(4);

        assertThat(d.getStatus()).isEqualTo(StatusDoacao.CANCELADA);
    }
}