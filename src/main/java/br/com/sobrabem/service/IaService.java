package br.com.sobrabem.service;

import br.com.sobrabem.ai.IaIndisponivelException;
import br.com.sobrabem.ai.OrientacaoResultado;
import br.com.sobrabem.ai.OrientacaoService;
import br.com.sobrabem.domain.entity.Categoria;
import br.com.sobrabem.dto.request.OrientacaoRequest;
import br.com.sobrabem.dto.response.OrientacaoResponse;
import br.com.sobrabem.exception.NegocioException;
import br.com.sobrabem.repository.CategoriaRepository;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.*;

/**
 * Lado Java da IA: valida a categoria, aplica o timeout e converte qualquer falha em 503.
 * A IA em si fica atrás da interface OrientacaoService.
 */
@Service
public class IaService {

    private final OrientacaoService orientacaoService;
    private final CategoriaRepository categorias;
    private final Duration timeout;
    // thread virtual: barata, e só fica esperando a resposta do Gemini pela rede
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public IaService(OrientacaoService orientacaoService, CategoriaRepository categorias,
                     @Value("${sobrabem.ia.timeout:20s}") Duration timeout) {
        this.orientacaoService = orientacaoService;
        this.categorias = categorias;
        this.timeout = timeout;
    }

    public OrientacaoResponse gerar(OrientacaoRequest req) {
        // aqui o contrato pede 404 (na doação, categoria inexistente é 400 no campo)
        Categoria categoria = categorias.findById(req.categoriaId())
                .orElseThrow(() -> NegocioException.naoEncontrado("Categoria não encontrada"));

        Future<OrientacaoResultado> tarefa = executor.submit(
                () -> orientacaoService.gerar(req.titulo().trim(), req.descricao(), categoria));
        try {
            return OrientacaoResponse.de(tarefa.get(timeout.toMillis(), TimeUnit.MILLISECONDS));
        } catch (TimeoutException e) {
            tarefa.cancel(true);
            throw new IaIndisponivelException("A IA não respondeu em " + timeout.toSeconds() + " s", e);
        } catch (ExecutionException e) {
            // qualquer erro da implementação (Gemini fora, cota, bug) vira 503
            if (e.getCause() instanceof IaIndisponivelException ia) throw ia;
            throw new IaIndisponivelException("Falha ao gerar a orientação", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IaIndisponivelException("Geração interrompida", e);
        }
    }

    @PreDestroy
    void encerrar() {
        executor.shutdownNow();
    }
}