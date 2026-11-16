package br.com.sobrabem.ai;

import br.com.sobrabem.domain.entity.Categoria;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Resposta fixa, para o app e a API funcionarem antes da IA ficar pronta.
 * Marcas no título simulam os outros resultados:
 *   #aviso → sem base no acervo | #erro → 503 | #lento → passa do timeout (503)
 */
@Component
@ConditionalOnProperty(name = "sobrabem.ia.modo", havingValue = "fake", matchIfMissing = true)
public class OrientacaoServiceFake implements OrientacaoService {

    @Override
    public OrientacaoResultado gerar(String titulo, String descricao, Categoria categoria) {
        String marca = titulo.toLowerCase(Locale.ROOT);

        if (marca.contains("#aviso")) {
            return new OrientacaoResultado(null, List.of(), "Sem orientação no acervo para este alimento.");
        }
        if (marca.contains("#erro")) {
            throw new IaIndisponivelException("Falha simulada da IA");
        }
        if (marca.contains("#lento")) {
            esperar(25_000);
        }

        return new OrientacaoResultado(
                "[FAKE] Para \"%s\" (%s): mantenha em embalagem fechada, em local seco e protegido do sol, "
                        .formatted(titulo, categoria.getNome())
                        + "e combine a retirada ainda hoje. Não doe se houver sinais de deterioração.",
                List.of("orientacao-fake.md"),
                null);
    }

    private static void esperar(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // o IaService cancela a tarefa quando estoura o timeout
            Thread.currentThread().interrupt();
        }
    }
}