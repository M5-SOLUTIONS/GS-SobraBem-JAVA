package br.com.sobrabem;

import br.com.sobrabem.domain.entity.Categoria;
import br.com.sobrabem.domain.entity.Doacao;
import br.com.sobrabem.domain.entity.Reserva;
import br.com.sobrabem.domain.entity.Usuario;
import br.com.sobrabem.domain.enums.Papel;
import br.com.sobrabem.domain.enums.Unidade;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

public final class Fixtures {

    private Fixtures() {
    }

    public static Usuario usuario(Long id, Papel papel) {
        String endereco = papel == Papel.DOADOR ? "Rua Teste, 100" : null;
        Usuario u = new Usuario("Usuario " + id, "u" + id + "@teste.com", "hash", "11987654321",
                papel, endereco, "São Paulo", "SP");
        definir(u, "id", id);
        return u;
    }

    public static Categoria categoria(Long id, int prazoMaxHoras) {
        Categoria c = new Categoria("Categoria " + id, null, prazoMaxHoras);
        definir(c, "id", id);
        return c;
    }

    public static Doacao doacao(Long id, Usuario doador, Categoria categoria, int total, Instant prazo) {
        Doacao d = new Doacao(doador, categoria, "Pão francês", null, total, Unidade.UNIDADE, prazo, null, false);
        definir(d, "id", id);
        return d;
    }

    public static Reserva reserva(Long id, Doacao doacao, Usuario receptor, int quantidade, String codigo) {
        Reserva r = new Reserva(doacao, receptor, quantidade, codigo);
        definir(r, "id", id);
        return r;
    }

    public static void definir(Object alvo, String campo, Object valor) {
        ReflectionTestUtils.setField(alvo, campo, valor);
    }
}