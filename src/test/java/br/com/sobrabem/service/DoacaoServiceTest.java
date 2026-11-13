package br.com.sobrabem.service;

import br.com.sobrabem.domain.entity.Categoria;
import br.com.sobrabem.domain.entity.Doacao;
import br.com.sobrabem.domain.entity.Reserva;
import br.com.sobrabem.domain.entity.Usuario;
import br.com.sobrabem.domain.enums.Papel;
import br.com.sobrabem.domain.enums.StatusDoacao;
import br.com.sobrabem.domain.enums.StatusReserva;
import br.com.sobrabem.domain.enums.Unidade;
import br.com.sobrabem.dto.request.DoacaoRequest;
import br.com.sobrabem.dto.response.DoacaoResponse;
import br.com.sobrabem.exception.CampoErro;
import br.com.sobrabem.exception.CodigoErro;
import br.com.sobrabem.exception.NegocioException;
import br.com.sobrabem.repository.CategoriaRepository;
import br.com.sobrabem.repository.DoacaoRepository;
import br.com.sobrabem.repository.ReservaRepository;
import br.com.sobrabem.repository.UsuarioRepository;
import br.com.sobrabem.security.Autenticado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static br.com.sobrabem.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoacaoServiceTest {

    @Mock
    DoacaoRepository doacoes;
    @Mock
    CategoriaRepository categorias;
    @Mock
    UsuarioRepository usuarios;
    @Mock
    ReservaRepository reservas;
    @InjectMocks
    DoacaoService service;

    private final Usuario doador = usuario(1L, Papel.DOADOR);
    private final Categoria paes = categoria(1L, 12);

    private static DoacaoRequest request(Long categoriaId, int total, Duration daquiA) {
        return new DoacaoRequest("Pão francês", null, categoriaId, total, Unidade.UNIDADE,
                OffsetDateTime.now(ZoneOffset.ofHours(-3)).plus(daquiA), null, null);
    }

    private Doacao doacaoDoDoador() {
        return doacao(10L, doador, paes, 10, Instant.now().plus(2, ChronoUnit.HOURS));
    }

    @Test
    void publicarComPrazoEmMenosDe30MinutosDa400() {
        when(categorias.findById(1L)).thenReturn(Optional.of(paes));

        NegocioException e = assertThrows(NegocioException.class,
                () -> service.publicar(1L, request(1L, 10, Duration.ofMinutes(10))));

        assertThat(e.getCodigo()).isEqualTo(CodigoErro.PRAZO_INVALIDO);
        verify(doacoes, never()).save(any());
    }

    @Test
    void publicarComPrazoAcimaDoLimiteDaCategoriaDa400() {
        when(categorias.findById(1L)).thenReturn(Optional.of(paes)); // limite 12 h

        NegocioException e = assertThrows(NegocioException.class,
                () -> service.publicar(1L, request(1L, 10, Duration.ofHours(13))));

        assertThat(e.getCodigo()).isEqualTo(CodigoErro.PRAZO_INVALIDO);
    }

    @Test
    void publicarComCategoriaInexistenteDa400NoCampo() {
        when(categorias.findById(99L)).thenReturn(Optional.empty());

        NegocioException e = assertThrows(NegocioException.class,
                () -> service.publicar(1L, request(99L, 10, Duration.ofHours(2))));

        assertThat(e.getCodigo()).isEqualTo(CodigoErro.VALIDACAO);
        assertThat(e.getErros()).extracting(CampoErro::campo).containsExactly("categoriaId");
    }

    @Test
    void publicarValidaComecaAtivaComSaldoCheio() {
        when(categorias.findById(1L)).thenReturn(Optional.of(paes));
        when(usuarios.findById(1L)).thenReturn(Optional.of(doador));
        DoacaoRequest req = request(1L, 30, Duration.ofHours(2));

        DoacaoResponse r = service.publicar(1L, req);

        assertThat(r.status()).isEqualTo(StatusDoacao.ATIVA);
        assertThat(r.quantidadeDisponivel()).isEqualTo(30);
        assertThat(r.geradaPorIa()).isFalse();
        assertThat(r.prazoRetirada()).isEqualTo(req.prazoRetirada().toInstant());
        verify(doacoes).save(any(Doacao.class));
    }

    @Test
    void editarDoacaoDeOutroDoadorDa403() {
        when(doacoes.buscarParaAtualizar(10L)).thenReturn(Optional.of(doacaoDoDoador()));

        NegocioException e = assertThrows(NegocioException.class,
                () -> service.atualizar(2L, 10L, request(1L, 10, Duration.ofHours(2))));

        assertThat(e.getCodigo()).isEqualTo(CodigoErro.ACESSO_NEGADO);
    }

    @Test
    void editarDoacaoCanceladaDa409() {
        Doacao d = doacaoDoDoador();
        d.cancelar();
        when(doacoes.buscarParaAtualizar(10L)).thenReturn(Optional.of(d));

        NegocioException e = assertThrows(NegocioException.class,
                () -> service.atualizar(1L, 10L, request(1L, 10, Duration.ofHours(2))));

        assertThat(e.getCodigo()).isEqualTo(CodigoErro.ESTADO_INVALIDO);
    }

    @Test
    void editarComTotalAbaixoDoReservadoDa400() {
        Doacao d = doacaoDoDoador();
        definir(d, "quantidadeDisponivel", 4); // 6 reservadas
        when(doacoes.buscarParaAtualizar(10L)).thenReturn(Optional.of(d));
        when(categorias.findById(1L)).thenReturn(Optional.of(paes));

        NegocioException e = assertThrows(NegocioException.class,
                () -> service.atualizar(1L, 10L, request(1L, 5, Duration.ofHours(2))));

        assertThat(e.getErros()).extracting(CampoErro::campo).containsExactly("quantidadeTotal");
    }

    @Test
    void cancelarCancelaAsReservasAtivas() {
        Doacao d = doacaoDoDoador();
        Reserva r1 = reserva(40L, d, usuario(5L, Papel.RECEPTOR), 2, "AAAAAA");
        Reserva r2 = reserva(41L, d, usuario(6L, Papel.RECEPTOR), 3, "BBBBBB");
        when(doacoes.buscarParaAtualizar(10L)).thenReturn(Optional.of(d));
        when(reservas.findByDoacaoIdAndStatus(10L, StatusReserva.ATIVA)).thenReturn(List.of(r1, r2));

        DoacaoResponse r = service.cancelar(1L, 10L);

        assertThat(r.status()).isEqualTo(StatusDoacao.CANCELADA);
        assertThat(r1.getStatus()).isEqualTo(StatusReserva.CANCELADA);
        assertThat(r2.getStatus()).isEqualTo(StatusReserva.CANCELADA);
    }

    @Test
    void excluirComReservasDa409() {
        when(doacoes.findById(10L)).thenReturn(Optional.of(doacaoDoDoador()));
        when(reservas.existsByDoacaoId(10L)).thenReturn(true);

        NegocioException e = assertThrows(NegocioException.class,
                () -> service.excluir(new Autenticado(1L, Papel.DOADOR), 10L));

        assertThat(e.getCodigo()).isEqualTo(CodigoErro.REGISTRO_EM_USO);
        verify(doacoes, never()).delete(any());
    }

    @Test
    void excluirDoacaoDeOutroDoadorDa403() {
        when(doacoes.findById(10L)).thenReturn(Optional.of(doacaoDoDoador()));

        NegocioException e = assertThrows(NegocioException.class,
                () -> service.excluir(new Autenticado(2L, Papel.DOADOR), 10L));

        assertThat(e.getCodigo()).isEqualTo(CodigoErro.ACESSO_NEGADO);
    }

    @Test
    void adminExcluiDoacaoDeQualquerDoador() {
        Doacao d = doacaoDoDoador();
        when(doacoes.findById(10L)).thenReturn(Optional.of(d));
        when(reservas.existsByDoacaoId(10L)).thenReturn(false);

        service.excluir(new Autenticado(99L, Papel.ADMIN), 10L);

        verify(doacoes).delete(d);
    }
}