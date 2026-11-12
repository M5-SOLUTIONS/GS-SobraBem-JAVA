package br.com.sobrabem.service;

import br.com.sobrabem.domain.entity.Doacao;
import br.com.sobrabem.domain.entity.Reserva;
import br.com.sobrabem.domain.entity.Usuario;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
public class ReservaService {

    private final ReservaRepository reservas;
    private final DoacaoRepository doacoes;
    private final UsuarioRepository usuarios;

    public ReservaService(ReservaRepository reservas, DoacaoRepository doacoes, UsuarioRepository usuarios) {
        this.reservas = reservas;
        this.doacoes = doacoes;
        this.usuarios = usuarios;
    }

    @Transactional
    public ReservaResponse reservar(Long receptorId, ReservaRequest req) {
        Long doacaoId = req.doacaoId();
        int quantidade = req.quantidade();

        if (!doacoes.existsById(doacaoId)) throw NegocioException.naoEncontrado("Doação não encontrada");
        if (reservas.existsByDoacaoIdAndReceptorIdAndStatus(doacaoId, receptorId, StatusReserva.ATIVA)) {
            throw reservaDuplicada();
        }

        Instant agora = Instant.now();
        if (doacoes.descontarSaldo(doacaoId, quantidade, agora) == 0) {
            throw motivoDaRecusa(doacaoId, agora);
        }

        Doacao doacao = doacoes.findById(doacaoId).orElseThrow();
        Usuario receptor = usuarios.findById(receptorId)
                .orElseThrow(() -> NegocioException.naoEncontrado("Usuário não encontrado"));
        Reserva reserva = new Reserva(doacao, receptor, quantidade, CodigoRetirada.gerar());
        try {
            reservas.saveAndFlush(reserva);
        } catch (DataIntegrityViolationException e) {
            // duas requisições simultâneas do mesmo receptor: o índice da V2 barra a segunda,
            // e o rollback desfaz o desconto de saldo feito acima
            if (violou(e, "SB_UX_RESERVA_ATIVA")) throw reservaDuplicada();
            throw e;
        }
        return ReservaResponse.de(reserva, true);
    }

    @Transactional(readOnly = true)
    public Page<ReservaResponse> minhas(Long receptorId, StatusReserva status, Pageable pageable) {
        return reservas.buscarDoReceptor(receptorId, status, pageable).map(r -> ReservaResponse.de(r, true));
    }

    @Transactional
    public ReservaResponse cancelar(Long receptorId, Long id) {
        Reserva reserva = travarComDoacao(id);
        if (!reserva.pertenceA(receptorId)) throw NegocioException.acessoNegado("A reserva não é sua");
        if (reserva.getStatus() == StatusReserva.CANCELADA) return ReservaResponse.de(reserva, true);
        if (reserva.getStatus() != StatusReserva.ATIVA) {
            throw NegocioException.estadoInvalido("Só reservas ATIVA podem ser canceladas");
        }
        if (!reserva.getDoacao().getPrazoRetirada().isAfter(Instant.now())) {
            throw NegocioException.estadoInvalido("O prazo de retirada já venceu");
        }

        reserva.cancelar();
        reserva.getDoacao().devolverSaldo(reserva.getQuantidade());
        return ReservaResponse.de(reserva, true);
    }

    @Transactional
    public ReservaResponse confirmarRetirada(Long doadorId, Long id, RetiradaRequest req) {
        Reserva reserva = travarComDoacao(id);
        if (!reserva.getDoacao().pertenceA(doadorId)) {
            throw NegocioException.acessoNegado("A reserva não é de uma doação sua");
        }
        if (reserva.getStatus() != StatusReserva.ATIVA) {
            throw NegocioException.estadoInvalido("Só reservas ATIVA podem ser retiradas");
        }
        // aceita minúsculas: o doador digita o que o receptor fala ou mostra
        if (!reserva.getCodigoRetirada().equalsIgnoreCase(req.codigoRetirada().trim())) {
            throw new NegocioException(CodigoErro.CODIGO_RETIRADA_INVALIDO, "O código de retirada não confere");
        }
        reserva.confirmarRetirada();
        return ReservaResponse.de(reserva, false);
    }

    /** ADMIN. Se a reserva ainda estava ATIVA, o saldo volta para a doação. */
    @Transactional
    public void excluir(Long id) {
        Reserva reserva = travarComDoacao(id);
        if (reserva.getStatus() == StatusReserva.ATIVA) {
            reserva.getDoacao().devolverSaldo(reserva.getQuantidade());
        }
        reservas.delete(reserva);
    }

    /**
     * Trava a linha da doação (SELECT ... FOR UPDATE) antes de ler a reserva.
     * Assim, operações simultâneas na mesma doação ficam em fila, sempre na mesma ordem
     * (doação, depois reserva), o que evita deadlock.
     */
    private Reserva travarComDoacao(Long reservaId) {
        Long doacaoId = reservas.buscarIdDaDoacao(reservaId)
                .orElseThrow(() -> NegocioException.naoEncontrado("Reserva não encontrada"));
        doacoes.buscarParaAtualizar(doacaoId);
        // a doação travada já está na memória do Hibernate: reserva.getDoacao() devolve essa mesma instância
        return reservas.findById(reservaId)
                .orElseThrow(() -> NegocioException.naoEncontrado("Reserva não encontrada"));
    }

    /** O UPDATE afetou 0 linhas: relê a doação para dizer o motivo certo. */
    private NegocioException motivoDaRecusa(Long doacaoId, Instant agora) {
        Doacao doacao = doacoes.findById(doacaoId).orElseThrow();
        if (doacao.getStatus() != StatusDoacao.ATIVA || !doacao.getPrazoRetirada().isAfter(agora)) {
            return new NegocioException(CodigoErro.DOACAO_INDISPONIVEL,
                    "A doação não está ativa ou o prazo de retirada venceu");
        }
        return new NegocioException(CodigoErro.SALDO_INSUFICIENTE,
                "Quantidade maior que o disponível (" + doacao.getQuantidadeDisponivel() + ")");
    }

    private static NegocioException reservaDuplicada() {
        return new NegocioException(CodigoErro.RESERVA_DUPLICADA, "Você já tem uma reserva ativa nesta doação");
    }

    private static boolean violou(DataIntegrityViolationException e, String constraint) {
        String mensagem = e.getMostSpecificCause().getMessage();
        return mensagem != null && mensagem.toUpperCase(Locale.ROOT).contains(constraint);
    }
}