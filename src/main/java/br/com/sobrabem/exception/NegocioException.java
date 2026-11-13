package br.com.sobrabem.exception;

import java.util.List;

public class NegocioException extends RuntimeException {

    private final CodigoErro codigo;
    private final List<CampoErro> erros;

    public NegocioException(CodigoErro codigo, String detalhe) {
        this(codigo, detalhe, List.of());
    }

    public NegocioException(CodigoErro codigo, String detalhe, List<CampoErro> erros) {
        super(detalhe);
        this.codigo = codigo;
        this.erros = List.copyOf(erros);
    }

    /** Regras que cruzam campos (validadas no service) saem no mesmo formato do Bean Validation. */
    public static NegocioException validacao(String campo, String mensagem) {
        return validacao(List.of(new CampoErro(campo, mensagem)));
    }

    public static NegocioException validacao(List<CampoErro> erros) {
        return new NegocioException(CodigoErro.VALIDACAO, descreverQuantidade(erros.size()), erros);
    }

    public static NegocioException naoEncontrado(String detalhe) {
        return new NegocioException(CodigoErro.NAO_ENCONTRADO, detalhe);
    }

    public static NegocioException acessoNegado(String detalhe) {
        return new NegocioException(CodigoErro.ACESSO_NEGADO, detalhe);
    }

    public static NegocioException estadoInvalido(String detalhe) {
        return new NegocioException(CodigoErro.ESTADO_INVALIDO, detalhe);
    }

    static String descreverQuantidade(int quantidade) {
        return quantidade == 1 ? "1 campo inválido" : quantidade + " campos inválidos";
    }

    public CodigoErro getCodigo() { return codigo; }
    public List<CampoErro> getErros() { return erros; }
}