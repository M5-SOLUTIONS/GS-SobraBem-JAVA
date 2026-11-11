package br.com.sobrabem.domain.enums;

public enum TipoPerfil {
    PADARIA(Papel.DOADOR),
    FEIRA(Papel.DOADOR),
    MERCADO(Papel.DOADOR),
    ONG(Papel.RECEPTOR),
    FAMILIA(Papel.RECEPTOR);

    private final Papel papel;

    TipoPerfil(Papel papel) {
        this.papel = papel;
    }

    public boolean compativelCom(Papel papel) {
        return this.papel == papel;
    }
}