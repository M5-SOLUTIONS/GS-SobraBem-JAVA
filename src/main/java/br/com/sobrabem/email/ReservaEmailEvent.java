package br.com.sobrabem.email;

import br.com.sobrabem.domain.entity.Doacao;
import br.com.sobrabem.domain.entity.Reserva;
import br.com.sobrabem.domain.entity.Usuario;
import br.com.sobrabem.domain.enums.Papel;
import br.com.sobrabem.domain.enums.Unidade;

import java.time.Instant;

public record ReservaEmailEvent(
        Tipo tipo,
        Long reservaId,
        Pessoa doador,
        Pessoa receptor,
        DoacaoInfo doacao,
        Integer quantidade,
        String codigoRetirada,
        Papel canceladoPor) {

    public enum Tipo { RESERVA_CRIADA, RESERVA_CANCELADA }

    public record Pessoa(String nome, String email) {
    }

    public record DoacaoInfo(String titulo, Unidade unidade, Instant prazoRetirada, String endereco) {
    }

    public static ReservaEmailEvent criada(Reserva reserva) {
        return montar(Tipo.RESERVA_CRIADA, reserva, null);
    }

    public static ReservaEmailEvent cancelada(Reserva reserva, Papel canceladoPor) {
        return montar(Tipo.RESERVA_CANCELADA, reserva, canceladoPor);
    }

    private static ReservaEmailEvent montar(Tipo tipo, Reserva r, Papel canceladoPor) {
        Doacao d = r.getDoacao();
        Usuario doador = d.getDoador();
        Usuario receptor = r.getReceptor();
        return new ReservaEmailEvent(
                tipo, r.getId(),
                new Pessoa(doador.getNome(), doador.getEmail()),
                new Pessoa(receptor.getNome(), receptor.getEmail()),
                new DoacaoInfo(d.getTitulo(), d.getUnidade(), d.getPrazoRetirada(), doador.getEndereco()),
                r.getQuantidade(), r.getCodigoRetirada(), canceladoPor);
    }
}