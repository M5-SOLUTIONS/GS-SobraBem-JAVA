package br.com.sobrabem.ai;

/** Gemini fora do ar, lento demais ou sem cota → 503 IA_INDISPONIVEL. */
public class IaIndisponivelException extends RuntimeException {

    public IaIndisponivelException(String mensagem) {
        super(mensagem);
    }

    public IaIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}