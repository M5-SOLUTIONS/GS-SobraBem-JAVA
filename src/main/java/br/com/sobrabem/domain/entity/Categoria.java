package br.com.sobrabem.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sb_t_categoria")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Long id;

    private String nome;
    private String descricao;
    private Integer prazoMaxHoras;

    public Categoria(String nome, String descricao, Integer prazoMaxHoras) {
        atualizar(nome, descricao, prazoMaxHoras);
    }

    public void atualizar(String nome, String descricao, Integer prazoMaxHoras) {
        this.nome = nome;
        this.descricao = descricao;
        this.prazoMaxHoras = prazoMaxHoras;
    }
}