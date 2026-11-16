package br.com.sobrabem.ai;

import br.com.sobrabem.domain.entity.Categoria;

/**
 * Fronteira entre a API (Java) e a IA.
 * Implementações: OrientacaoServiceFake (IA_MODO=fake) e a real com Gemini (IA_MODO=gemini).
 * Em caso de falha, lance IaIndisponivelException ou qualquer RuntimeException: vira 503.
 * O timeout de 20 s é aplicado pelo IaService; a implementação não precisa tratá-lo.
 */
public interface OrientacaoService {

    OrientacaoResultado gerar(String titulo, String descricao, Categoria categoria);
}