package br.com.sobrabem.exception;

import org.springframework.http.HttpStatus;

import java.util.Locale;

public enum CodigoErro {
    VALIDACAO(HttpStatus.BAD_REQUEST, "Dados inválidos"),
    PRAZO_INVALIDO(HttpStatus.BAD_REQUEST, "Prazo de retirada inválido"),
    CODIGO_RETIRADA_INVALIDO(HttpStatus.BAD_REQUEST, "Código de retirada inválido"),
    NAO_AUTENTICADO(HttpStatus.UNAUTHORIZED, "Não autenticado"),
    CREDENCIAIS_INVALIDAS(HttpStatus.UNAUTHORIZED, "Credenciais inválidas"),
    ACESSO_NEGADO(HttpStatus.FORBIDDEN, "Acesso negado"),
    NAO_ENCONTRADO(HttpStatus.NOT_FOUND, "Recurso não encontrado"),
    EMAIL_EM_USO(HttpStatus.CONFLICT, "E-mail já cadastrado"),
    CATEGORIA_DUPLICADA(HttpStatus.CONFLICT, "Categoria já existe"),
    REGISTRO_EM_USO(HttpStatus.CONFLICT, "Registro em uso"),
    SALDO_INSUFICIENTE(HttpStatus.CONFLICT, "Saldo insuficiente"),
    DOACAO_INDISPONIVEL(HttpStatus.CONFLICT, "Doação indisponível"),
    RESERVA_DUPLICADA(HttpStatus.CONFLICT, "Reserva duplicada"),
    ESTADO_INVALIDO(HttpStatus.CONFLICT, "Operação não permitida no status atual"),
    ERRO_INTERNO(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno"),
    IA_INDISPONIVEL(HttpStatus.SERVICE_UNAVAILABLE, "IA indisponível");

    private final HttpStatus status;
    private final String titulo;

    CodigoErro(HttpStatus status, String titulo) {
        this.status = status;
        this.titulo = titulo;
    }

    public HttpStatus getStatus() { return status; }
    public String getTitulo() { return titulo; }

    /** CATEGORIA_DUPLICADA → categoria-duplicada (vai no "type" do erro). */
    public String slug() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}