package br.com.sobrabem.ai;

import java.util.List;

/**
 * Com base no corpus: orientacao preenchida, fontes com os arquivos usados, aviso null.
 * Sem base no corpus: orientacao null, fontes vazia, aviso com a mensagem.
 */
public record OrientacaoResultado(String orientacao, List<String> fontes, String aviso) {
}