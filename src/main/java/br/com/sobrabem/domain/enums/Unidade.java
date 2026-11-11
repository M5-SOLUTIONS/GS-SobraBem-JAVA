package br.com.sobrabem.domain.enums;

public enum Unidade {
    UNIDADE("unidade(s)"),
    KG("kg"),
    PACOTE("pacote(s)");

    private final String rotulo;

    Unidade(String rotulo) {
        this.rotulo = rotulo;
    }

    /** Texto usado nos e-mails ("8 unidade(s)"). */
    public String rotulo() {
        return rotulo;
    }
}