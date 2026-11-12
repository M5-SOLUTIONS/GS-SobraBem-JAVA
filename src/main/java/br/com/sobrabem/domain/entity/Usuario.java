package br.com.sobrabem.domain.entity;

import br.com.sobrabem.domain.converter.SimNaoConverter;
import br.com.sobrabem.domain.enums.Papel;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "sb_t_usuario")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;

    private String nome;
    private String email;
    private String senhaHash;
    private String telefone;

    @Enumerated(EnumType.STRING)
    private Papel papel;

    private String endereco;
    private String cidade;

    // CHAR no Oracle: sem o columnDefinition, o validate esperaria VARCHAR2 e a API não subiria
    @Column(columnDefinition = "char(2)")
    private String uf;

    @Convert(converter = SimNaoConverter.class)
    @Column(columnDefinition = "char(1)")
    private Boolean ativo;

    private Instant criadoEm;

    public Usuario(String nome, String email, String senhaHash, String telefone, Papel papel,
                   String endereco, String cidade, String uf) {
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.telefone = telefone;
        this.papel = papel;
        this.endereco = endereco;
        this.cidade = cidade;
        this.uf = uf;
        this.ativo = true;
    }

    /** criado_em em UTC: o DEFAULT SYSTIMESTAMP gravaria no fuso do servidor Oracle. */
    @PrePersist
    void antesDeInserir() {
        if (criadoEm == null) criadoEm = Instant.now();
        if (ativo == null) ativo = true;
    }

    public void atualizarPerfil(String nome, String telefone, String endereco, String cidade, String uf) {
        this.nome = nome;
        this.telefone = telefone;
        this.endereco = endereco;
        this.cidade = cidade;
        this.uf = uf;
    }

    public boolean isAtivo() {
        return Boolean.TRUE.equals(ativo);
    }
}