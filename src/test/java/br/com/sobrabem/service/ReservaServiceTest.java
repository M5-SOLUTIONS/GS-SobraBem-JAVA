package br.com.sobrabem.service;

import br.com.sobrabem.domain.entity.Categoria;
import br.com.sobrabem.domain.entity.Doacao;
import br.com.sobrabem.domain.entity.Reserva;
import br.com.sobrabem.domain.entity.Usuario;
import br.com.sobrabem.domain.enums.Papel;
import br.com.sobrabem.domain.enums.StatusDoacao;
import br.com.sobrabem.domain.enums.StatusReserva;
import br.com.sobrabem.dto.request.ReservaRequest;
import br.com.sobrabem.dto.request.RetiradaRequest;
import br.com.sobrabem.dto.response.ReservaResponse;
import br.com.sobrabem.exception.CodigoErro;
import br.com.sobrabem.exception.NegocioException;
import br.com.sobrabem.repository.DoacaoRepository;
import br.com.sobrabem.repository.ReservaRepository;
import br.com.sobrabem.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static br.com.sobrabem.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    ReservaRepository reservas;
    @Mock
    DoacaoRepository doacoes;
    @Mock
    UsuarioRepository usuarios;
    @InjectMocks
    ReservaService service;

    private final Usuario doador = usuario(1L, Papel.DOADOR);
    private final Usuario receptor = usuario(5L, Papel.RECEPTOR);
    private final Categoria paes = categoria(1L, 12);
    private final Doacao doacao = doacao(10L, doador, paes, 10, Instant.now().plus(2, ChronoUnit.HOURS));

    private void prepararTrava(Reserva reserva) {
        Long doacaoId = reserva.getDoacao().getId();
        when(reservas.buscarIdDaDoacao(reserva.getId())).thenReturn(Optional.of(doacaoId));
        when(doacoes.buscarParaAtualizar(doacaoId)).thenReturn(Optional.of(reserva.getDoacao()));
        when(reservas.findById(reserva.getId())).thenReturn(Optional.of(reserva));
    }

    private void prepararReserva() {
        when(doacoes.existsById(10L)).thenReturn(true);
        when(reservas.existsByDoacaoIdAndReceptorIdAndStatus(10L, 5L, StatusReserva.ATIVA)).thenReturn(false);
    }

    @Test
    void reservarDoacaoInexistenteDa404() {
        when(doacoes.existsById(10L)).thenReturn(false);

        NegocioException e = assertThrows(NegocioException.class,
                () -> service.reservar(5L, new ReservaRequest(10L, 1)));

        assertThat(e.getCodigo()).isEqualTo(CodigoErro.NAO_ENCONTRADO);
        verify(doacoes, never()).descontarSaldo(anyLong(), anyInt(), any());
    }

    @Test
    void reservarDeNovoNaMesmaDoacaoDa409() { // RN05
        when(doacoes.existsById(10L)).thenReturn(true);
        when(reservas.existsByDoacaoIdAndReceptorIdAndStatus(10L, 5L, StatusReserva.ATIVA)).thenReturn(true);

        NegocioException e = assertThrows(NegocioException.class,
                () -> service.reservar(5L, new ReservaRequest(10L, 1)));

        assertThat(e.getCodigo()).isEqualTo(CodigoErro.RESERVA_DUPLICADA);
        verify(doacoes, never()).descontarSaldo(anyLong(), anyInt(), any());
    }

    @Test
    void updateSemLinhasEmDoacaoAtivaEhSaldoInsuficiente() { // RN06
        prepararReserva();
        when(doacoes.descontarSaldo(eq(10L), eq(11), any(Instant.class))).thenReturn(0);
        when(doacoes.findById(10L)).thenReturn(Optional.of(doacao));

        NegocioException e = assertThrows(NegocioException.class,
                () -> service.reservar(5L, new ReservaRequest(10L, 11)));

        assertThat(e.getCodigo()).isEqualTo(CodigoErro.SALDO_INSUFICIENTE);
    }

    @Test
    void updateSemLinhasEmDoacaoEsgotadaEhDoacaoIndisponivel() {
        definir(doacao, "quantidadeDisponivel", 0);
        definir(doacao, "status", StatusDoacao.ESGOTADA);
        prepararReserva();
        when(doacoes.descontarSaldo(eq(10L), eq(1), any(Instant.class))).thenReturn(0);
        when(doacoes.findById(10L)).thenReturn(Optional.of(doacao));

        NegocioException e = assertThrows(NegocioException.class,
                () -> service.reservar(5L, new ReservaRequest(10L, 1)));

        assertThat(e.getCodigo()).isEqualTo(CodigoErro.DOACAO_INDISPONIVEL);
    }

    @Test
    void updateSemLinhasEmDoacaoVencidaEhDoacaoIndisponivel() {
        definir(doacao, "prazoRetirada", Instant.now().minus(1, ChronoUnit.MINUTES));
        prepararReserva();
        when(doacoes.descontarSaldo(eq(10L), eq(1), any(Instant.class))).thenReturn(0);
        when(doacoes.findById(10L)).thenReturn(Optional.of(doacao));

        NegocioException e = assertThrows(NegocioException.class,
                () -> service.reservar(5L, new ReservaRequest(10L, 1)));

        assertThat(e.getCodigo()).isEqualTo(CodigoErro.DOACAO_INDISPONIVEL);
    }

    @Test
    void indiceDeReservaAtivaVioladoViraReservaDuplicada() { // concorrência do mesmo receptor
        prepararReserva();
        when(doacoes.descontarSaldo(eq(10L), eq(1), any(Instant.class))).thenReturn(1);
        when(doacoes.findById(10L)).thenReturn(Optional.of(doacao));
        when(usuarios.findById(5L)).thenReturn(Optional.of(receptor));
        when(reservas.saveAndFlush(any(Reserva.class))).thenThrow(
                new DataIntegrityViolationException("ORA-00001: restrição exclusiva (RM.SB_UX_RESERVA_ATIVA) violada"));

        NegocioException e = assertThrows(NegocioException.class,
                () -> service.reservar(5L, new ReservaRequest(10L, 1)));

        assertThat(e.getCodigo()).isEqualTo(CodigoErro.RESERVA_DUPLICADA);
    }

    @Test
    void reservaValidaGeraCodigoEMostraParaOReceptor() { // RN07
        prepararReserva();
        when(doacoes.descontarSaldo(eq(10L), eq(4), any(Instant.class))).thenReturn(1);
        when(doacoes.findById(10L)).thenReturn(Optional.of(doacao));
        when(usuarios.findById(5L)).thenReturn(Optional.of(receptor));

        ReservaResponse r = service.reservar(5L, new ReservaRequest(10L, 4));

        assertThat(r.status()).isEqualTo(StatusReserva.ATIVA);
        assertThat(r.quantidade()).isEqualTo(4);
        assertThat(r.codigoRetirada()).matches("[A-HJ-NP-Z2-9]{6}"); // sem 0, O, 1 e I
        verify(reservas).saveAndFlush(any(Reserva.class));
    }

    @Test
    void cancelarDevolveOSaldoEReativaEsgotada() { // RN08
        definir(doacao, "quantidadeDisponivel", 0);
        definir(doacao, "status", StatusDoacao.ESGOTADA);
        Reserva reserva = reserva(40L, doacao, receptor, 4, "K7P2QX");
        prepararTrava(reserva);

        ReservaResponse r = service.cancelar(5L, 40L);

        assertThat(r.status()).isEqualTo(StatusReserva.CANCELADA);
        assertThat(doacao.getQuantidadeDisponivel()).isEqualTo(4);
        assertThat(doacao.getStatus()).isEqualTo(StatusDoacao.ATIVA);
    }

    @Test
    void cancelarDeNovoNaoDevolveOSaldoDuasVezes() {
        definir(doacao, "quantidadeDisponivel", 6);
        Reserva reserva = reserva(40L, doacao, receptor, 4, "K7P2QX");
        reserva.cancelar();
        prepararTrava(reserva);

        ReservaResponse r = service.cancelar(5L, 40L);

        assertThat(r.status()).isEqualTo(StatusReserva.CANCELADA);
        assertThat(doacao.getQuantidadeDisponivel()).isEqualTo(6);
    }

    @Test
    void cancelarReservaDeOutroReceptorDa403() {
        Reserva reserva = reserva(40L, doacao, receptor, 4, "K7P2QX");
        prepararTrava(reserva);

        NegocioException e = assertThrows(NegocioException.class, () -> service.cancelar(6L, 40L));

        assertThat(e.getCodigo()).isEqualTo(CodigoErro.ACESSO_NEGADO);
    }

    @Test
    void cancelarDepoisDoPrazoDa409() {
        definir(doacao, "prazoRetirada", Instant.now().minus(1, ChronoUnit.MINUTES));
        Reserva reserva = reserva(40L, doacao, receptor, 4, "K7P2QX");
        prepararTrava(reserva);

        NegocioException e = assertThrows(NegocioException.class, () -> service.cancelar(5L, 40L));

        assertThat(e.getCodigo()).isEqualTo(CodigoErro.ESTADO_INVALIDO);
    }

    @Test
    void retiradaComCodigoErradoDa400EMantemAReserva() {
        Reserva reserva = reserva(40L, doacao, receptor, 4, "K7P2QX");
        prepararTrava(reserva);

        NegocioException e = assertThrows(NegocioException.class,
                () -> service.confirmarRetirada(1L, 40L, new RetiradaRequest("AAAAAA")));

        assertThat(e.getCodigo()).isEqualTo(CodigoErro.CODIGO_RETIRADA_INVALIDO);
        assertThat(reserva.getStatus()).isEqualTo(StatusReserva.ATIVA);
    }

    @Test
    void retiradaAceitaCodigoEmMinusculasEEscondeOCodigo() {
        Reserva reserva = reserva(40L, doacao, receptor, 4, "K7P2QX");
        prepararTrava(reserva);

        ReservaResponse r = service.confirmarRetirada(1L, 40L, new RetiradaRequest(" k7p2qx "));

        assertThat(r.status()).isEqualTo(StatusReserva.RETIRADA);
        assertThat(r.retiradoEm()).isNotNull();
        assertThat(r.codigoRetirada()).isNull();
    }

    @Test
    void retiradaPorOutroDoadorDa403() {
        Reserva reserva = reserva(40L, doacao, receptor, 4, "K7P2QX");
        prepararTrava(reserva);

        NegocioException e = assertThrows(NegocioException.class,
                () -> service.confirmarRetirada(2L, 40L, new RetiradaRequest("K7P2QX")));

        assertThat(e.getCodigo()).isEqualTo(CodigoErro.ACESSO_NEGADO);
    }

    @Test
    void retiradaDeReservaNaoAtivaDa409() {
        Reserva reserva = reserva(40L, doacao, receptor, 4, "K7P2QX");
        reserva.confirmarRetirada();
        prepararTrava(reserva);

        NegocioException e = assertThrows(NegocioException.class,
                () -> service.confirmarRetirada(1L, 40L, new RetiradaRequest("K7P2QX")));

        assertThat(e.getCodigo()).isEqualTo(CodigoErro.ESTADO_INVALIDO);
    }

    @Test
    void excluirReservaAtivaDevolveOSaldo() {
        definir(doacao, "quantidadeDisponivel", 6);
        Reserva reserva = reserva(40L, doacao, receptor, 4, "K7P2QX");
        prepararTrava(reserva);

        service.excluir(40L);

        assertThat(doacao.getQuantidadeDisponivel()).isEqualTo(10);
        verify(reservas).delete(reserva);
    }
}