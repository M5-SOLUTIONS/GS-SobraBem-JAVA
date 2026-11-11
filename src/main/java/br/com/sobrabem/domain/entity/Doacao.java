package br.com.sobrabem.domain.entity;

import br.com.sobrabem.domain.converter.SimNaoConverter;
import br.com.sobrabem.domain.enums.StatusDoacao;
import br.com.sobrabem.domain.enums.Unidade;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "sb_t_doacao")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Doacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_doacao")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_doador")
    private Usuario doador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_categoria")
    private Categoria categoria;

    private String titulo;
    private String descricao;
    private Integer quantidadeTotal;
    private Integer quantidadeDisponivel;

    @Enumerated(EnumType.STRING)
    private Unidade unidade;

    private Instant prazoRetirada;

    @Enumerated(EnumType.STRING)
    private StatusDoacao status;

    private String orientacoesConservacao;

    @Convert(converter = SimNaoConverter.class)
    @Column(columnDefinition = "char(1)")
    private Boolean geradaPorIa;

    private Instant criadoEm;

    /**ao publicar, disponível = total e status ATIVA. */
    public Doacao(Usuario doador, Categoria categoria, String titulo, String descricao, int quantidadeTotal,
                  Unidade unidade, Instant prazoRetirada, String orientacoesConservacao, boolean geradaPorIa) {
        this.doador = doador;
        this.categoria = categoria;
        this.titulo = titulo;
        this.descricao = descricao;
        this.quantidadeTotal = quantidadeTotal;
        this.quantidadeDisponivel = quantidadeTotal;
        this.unidade = unidade;
        this.prazoRetirada = prazoRetirada;
        this.status = StatusDoacao.ATIVA;
        this.orientacoesConservacao = orientacoesConservacao;
        this.geradaPorIa = geradaPorIa;
    }

    @PrePersist
    void antesDeInserir() {
        if (criadoEm == null) criadoEm = Instant.now();
    }

    public boolean pertenceA(Long usuarioId) {
        return doador.getId().equals(usuarioId);
    }

    /** ATIVA ou ESGOTADA: ainda pode ser editada ou cancelada. */
    public boolean isAberta() {
        return status == StatusDoacao.ATIVA || status == StatusDoacao.ESGOTADA;
    }

    public int getQuantidadeReservada() {
        return quantidadeTotal - quantidadeDisponivel;
    }

    public void atualizar(Categoria categoria, String titulo, String descricao, int novoTotal, Unidade unidade,
                          Instant prazoRetirada, String orientacoesConservacao, boolean geradaPorIa) {
        int reservada = getQuantidadeReservada();
        this.categoria = categoria;
        this.titulo = titulo;
        this.descricao = descricao;
        this.quantidadeTotal = novoTotal;
        this.quantidadeDisponivel = novoTotal - reservada;
        this.status = quantidadeDisponivel == 0 ? StatusDoacao.ESGOTADA : StatusDoacao.ATIVA;
        this.unidade = unidade;
        this.prazoRetirada = prazoRetirada;
        this.orientacoesConservacao = orientacoesConservacao;
        this.geradaPorIa = geradaPorIa;
    }

    public void devolverSaldo(int quantidade) {
        quantidadeDisponivel += quantidade;
        if (status == StatusDoacao.ESGOTADA) status = StatusDoacao.ATIVA;
    }

    public void cancelar() {
        status = StatusDoacao.CANCELADA;
    }

    public boolean isGeradaPorIa() {
        return Boolean.TRUE.equals(geradaPorIa);
    }
}