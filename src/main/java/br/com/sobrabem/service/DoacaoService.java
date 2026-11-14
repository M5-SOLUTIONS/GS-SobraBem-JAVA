package br.com.sobrabem.service;

import br.com.sobrabem.domain.entity.Categoria;
import br.com.sobrabem.domain.entity.Doacao;
import br.com.sobrabem.domain.entity.Reserva;
import br.com.sobrabem.domain.entity.Usuario;
import br.com.sobrabem.domain.enums.Papel;
import br.com.sobrabem.domain.enums.StatusDoacao;
import br.com.sobrabem.domain.enums.StatusReserva;
import br.com.sobrabem.dto.request.DoacaoRequest;
import br.com.sobrabem.dto.response.DoacaoResponse;
import br.com.sobrabem.dto.response.ReservaResponse;
import br.com.sobrabem.email.ReservaEmailEvent;
import br.com.sobrabem.exception.CodigoErro;
import br.com.sobrabem.exception.NegocioException;
import br.com.sobrabem.repository.CategoriaRepository;
import br.com.sobrabem.repository.DoacaoRepository;
import br.com.sobrabem.repository.ReservaRepository;
import br.com.sobrabem.repository.UsuarioRepository;
import br.com.sobrabem.security.Autenticado;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class DoacaoService {

    private static final Duration ANTECEDENCIA_MINIMA = Duration.ofMinutes(30);

    private final DoacaoRepository doacoes;
    private final CategoriaRepository categorias;
    private final UsuarioRepository usuarios;
    private final ReservaRepository reservas;
    private final ApplicationEventPublisher eventos;

    public DoacaoService(DoacaoRepository doacoes, CategoriaRepository categorias, UsuarioRepository usuarios,
                         ReservaRepository reservas, ApplicationEventPublisher eventos) {
        this.doacoes = doacoes;
        this.categorias = categorias;
        this.usuarios = usuarios;
        this.reservas = reservas;
        this.eventos = eventos;
    }

    @Transactional
    public DoacaoResponse publicar(Long doadorId, DoacaoRequest req) {
        Categoria categoria = buscarCategoria(req.categoriaId());
        Instant prazo = req.prazoRetirada().toInstant();
        validarPrazo(prazo, categoria);
        Usuario doador = usuarios.findById(doadorId)
                .orElseThrow(() -> NegocioException.naoEncontrado("Usuário não encontrado"));

        Doacao doacao = new Doacao(doador, categoria, req.titulo().trim(), req.descricao(), req.quantidadeTotal(),
                req.unidade(), prazo, req.orientacoesConservacao(), Boolean.TRUE.equals(req.geradaPorIa()));
        doacoes.save(doacao);
        return DoacaoResponse.de(doacao);
    }

    @Transactional(readOnly = true)
    public Page<DoacaoResponse> vitrine(Long categoriaId, String cidade, Pageable pageable) {
        String filtroCidade = (cidade == null || cidade.isBlank()) ? null : cidade.trim();
        return doacoes.buscarVitrine(Instant.now(), categoriaId, filtroCidade, pageable).map(DoacaoResponse::de);
    }

    @Transactional(readOnly = true)
    public Page<DoacaoResponse> minhas(Long doadorId, StatusDoacao status, Pageable pageable) {
        return doacoes.buscarDoDoador(doadorId, status, pageable).map(DoacaoResponse::de);
    }

    @Transactional(readOnly = true)
    public DoacaoResponse buscar(Long id) {
        return DoacaoResponse.de(buscarEntidade(id));
    }

    @Transactional
    public DoacaoResponse atualizar(Long doadorId, Long id, DoacaoRequest req) {
        Doacao doacao = travar(id);
        exigirDono(doacao, doadorId);
        if (!doacao.isAberta()) {
            throw NegocioException.estadoInvalido("Só doações ATIVA ou ESGOTADA podem ser editadas");
        }

        Categoria categoria = buscarCategoria(req.categoriaId());
        Instant prazo = req.prazoRetirada().toInstant();
        validarPrazo(prazo, categoria);

        int reservada = doacao.getQuantidadeReservada();
        if (req.quantidadeTotal() < reservada) {
            throw NegocioException.validacao("quantidadeTotal",
                    "não pode ser menor que o já reservado (" + reservada + ")");
        }
        doacao.atualizar(categoria, req.titulo().trim(), req.descricao(), req.quantidadeTotal(), req.unidade(),
                prazo, req.orientacoesConservacao(), Boolean.TRUE.equals(req.geradaPorIa()));
        return DoacaoResponse.de(doacao);
    }

    @Transactional
    public DoacaoResponse cancelar(Long doadorId, Long id) {
        Doacao doacao = travar(id);
        exigirDono(doacao, doadorId);
        if (!doacao.isAberta()) {
            throw NegocioException.estadoInvalido("Só doações ATIVA ou ESGOTADA podem ser canceladas");
        }
        doacao.cancelar();
        for (Reserva reserva : reservas.findByDoacaoIdAndStatus(id, StatusReserva.ATIVA)) {
            reserva.cancelar();
            eventos.publishEvent(ReservaEmailEvent.cancelada(reserva, Papel.DOADOR));
        }
        return DoacaoResponse.de(doacao);
    }

    @Transactional
    public void excluir(Autenticado usuario, Long id) {
        Doacao doacao = buscarEntidade(id);
        if (!usuario.isAdmin() && !doacao.pertenceA(usuario.id())) {
            throw NegocioException.acessoNegado("A doação não é sua");
        }
        if (reservas.existsByDoacaoId(id)) {
            throw new NegocioException(CodigoErro.REGISTRO_EM_USO, "A doação já tem reservas; use o cancelamento");
        }
        doacoes.delete(doacao);
    }

    @Transactional(readOnly = true)
    public Page<ReservaResponse> reservasDaDoacao(Long doadorId, Long id, StatusReserva status, Pageable pageable) {
        Doacao doacao = buscarEntidade(id);
        exigirDono(doacao, doadorId);
        return reservas.buscarDaDoacao(id, status, pageable).map(r -> ReservaResponse.de(r, false));
    }

    private void validarPrazo(Instant prazo, Categoria categoria) {
        Instant agora = Instant.now();
        Instant minimo = agora.plus(ANTECEDENCIA_MINIMA);
        Instant maximo = agora.plus(Duration.ofHours(categoria.getPrazoMaxHoras()));
        if (prazo.isBefore(minimo) || prazo.isAfter(maximo)) {
            throw new NegocioException(CodigoErro.PRAZO_INVALIDO,
                    "O prazo de retirada deve ficar entre 30 minutos e %d horas a partir de agora"
                            .formatted(categoria.getPrazoMaxHoras()));
        }
    }

    private Categoria buscarCategoria(Long categoriaId) {
        return categorias.findById(categoriaId)
                .orElseThrow(() -> NegocioException.validacao("categoriaId", "categoria não encontrada"));
    }

    private Doacao buscarEntidade(Long id) {
        return doacoes.findById(id).orElseThrow(() -> NegocioException.naoEncontrado("Doação não encontrada"));
    }

    private Doacao travar(Long id) {
        return doacoes.buscarParaAtualizar(id)
                .orElseThrow(() -> NegocioException.naoEncontrado("Doação não encontrada"));
    }

    private static void exigirDono(Doacao doacao, Long doadorId) {
        if (!doacao.pertenceA(doadorId)) throw NegocioException.acessoNegado("A doação não é sua");
    }
}