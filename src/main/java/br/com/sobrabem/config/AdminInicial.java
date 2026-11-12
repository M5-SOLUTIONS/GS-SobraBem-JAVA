package br.com.sobrabem.config;

import br.com.sobrabem.domain.entity.Usuario;
import br.com.sobrabem.domain.enums.Papel;
import br.com.sobrabem.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * ADMIN não se cadastra pela API. Na subida, se não existir nenhum ADMIN,
 * cria um com ADMIN_EMAIL e ADMIN_SENHA. A senha fica só na variável de ambiente.
 */
@Component
public class AdminInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicial.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String senha;

    public AdminInicial(UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
                        @Value("${sobrabem.admin.email:}") String email,
                        @Value("${sobrabem.admin.senha:}") String senha) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.senha = senha;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarios.existsByPapel(Papel.ADMIN)) return;
        if (email.isBlank() || senha.isBlank()) {
            log.warn("Não há ADMIN no banco e ADMIN_EMAIL/ADMIN_SENHA não foram definidos: nenhum ADMIN criado");
            return;
        }
        usuarios.save(new Usuario("Equipe SobraBem", email.trim().toLowerCase(Locale.ROOT),
                passwordEncoder.encode(senha), null, Papel.ADMIN, null, null, null));
        log.info("ADMIN inicial criado: {}", email);
    }
}