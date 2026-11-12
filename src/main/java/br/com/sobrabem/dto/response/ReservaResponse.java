package br.com.sobrabem.dto.response;

import br.com.sobrabem.domain.entity.Doacao;
import br.com.sobrabem.domain.entity.Reserva;
import br.com.sobrabem.domain.entity.Usuario;
import br.com.sobrabem.domain.enums.StatusDoacao;
import br.com.sobrabem.domain.enums.StatusReserva;
import br.com.sobrabem.domain.enums.Unidade;

import java.time.Instant;

public record ReservaResponse(
        Long id, DoacaoResumo doacao, ReceptorResumo receptor, Integer quantidade, StatusReserva status,
        String codigoRetirada, Instant criadoEm, Instant retiradoEm) {

    public record DoacaoResumo(Long id, String titulo, Unidade unidade, Instant prazoRetirada,
                               StatusDoacao status, DoadorResumo doador) {
    }

    public record ReceptorResumo(Long id, String nome, String telefone) {
    }

    /** mostrarCodigo: true só para o receptor dono; para o doador vai null (é ele quem confere). */
    public static ReservaResponse de(Reserva r, boolean mostrarCodigo) {
        Doacao d = r.getDoacao();
        Usuario receptor = r.getReceptor();
        return new ReservaResponse(
                r.getId(),
                new DoacaoResumo(d.getId(), d.getTitulo(), d.getUnidade(), d.getPrazoRetirada(), d.getStatus(),
                        DoadorResumo.de(d.getDoador())),
                new ReceptorResumo(receptor.getId(), receptor.getNome(), receptor.getTelefone()),
                r.getQuantidade(), r.getStatus(),
                mostrarCodigo ? r.getCodigoRetirada() : null,
                r.getCriadoEm(), r.getRetiradoEm());
    }
}