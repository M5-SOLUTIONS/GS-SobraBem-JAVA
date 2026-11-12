package br.com.sobrabem.service;

import br.com.sobrabem.domain.entity.Usuario;
import br.com.sobrabem.domain.enums.Papel;
import br.com.sobrabem.dto.request.LoginRequest;
import br.com.sobrabem.dto.request.RegistroRequest;
import br.com.sobrabem.dto.response.LoginResponse;
import br.com.sobrabem.dto.response.UsuarioResponse;
import br.com.sobrabem.exception.CampoErro;
import br.com.sobrabem.exception.CodigoErro;
import br.com.sobrabem.exception.NegocioException;
import br.com.sobrabem.repository.UsuarioRepository;
import br.com.sobrabem.security.TokenService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional
    public UsuarioResponse registrar(RegistroRequest req) {
        List<CampoErro> erros = new ArrayList<>();
        if (req.papel() == Papel.ADMIN) {
            erros.add(new CampoErro("papel", "ADMIN não pode se cadastrar pela API"));
        }
        if (req.papel() == Papel.DOADOR && isBlank(req.endereco())) {
            erros.add(new CampoErro("endereco", "é obrigatório para doador"));
        }
        if (!erros.isEmpty()) throw NegocioException.validacao(erros);

        String email = normalizarEmail(req.email());
        if (usuarios.existsByEmail(email)) throw emailEmUso();

        // endereço é o local de retirada: só faz sentido para doador
        String endereco = req.papel() == Papel.DOADOR ? req.endereco().trim() : null;
        Usuario usuario = new Usuario(req.nome().trim(), email, passwordEncoder.encode(req.senha()),
                req.telefone(), req.papel(), endereco, req.cidade().trim(), req.uf());
        try {
            usuarios.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            throw emailEmUso();
        }
        return UsuarioResponse.de(usuario);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        // e-mail inexistente, senha errada e usuário inativo dão a mesma resposta:
        // assim o login não revela quais e-mails estão cadastrados
        Usuario usuario = usuarios.findByEmail(normalizarEmail(req.email()))
                .filter(Usuario::isAtivo)
                .filter(u -> passwordEncoder.matches(req.senha(), u.getSenhaHash()))
                .orElseThrow(() -> new NegocioException(CodigoErro.CREDENCIAIS_INVALIDAS, "E-mail ou senha inválidos"));

        TokenService.TokenEmitido token = tokenService.emitir(usuario);
        return new LoginResponse(token.token(), "Bearer", token.expiraEm(),
                new LoginResponse.UsuarioResumo(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPapel()));
    }

    private static NegocioException emailEmUso() {
        return new NegocioException(CodigoErro.EMAIL_EM_USO, "Já existe um usuário com este e-mail");
    }

    static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    static boolean isBlank(String valor) {
        return valor == null || valor.isBlank();
    }
}