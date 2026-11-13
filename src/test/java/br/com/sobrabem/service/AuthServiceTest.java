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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static br.com.sobrabem.Fixtures.definir;
import static br.com.sobrabem.Fixtures.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UsuarioRepository usuarios;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    TokenService tokenService;
    @InjectMocks
    AuthService service;

    private static RegistroRequest registro(Papel papel, String endereco) {
        return new RegistroRequest("Padaria Teste", "  Contato@Teste.COM ", "Senha123", "11987654321",
                papel, endereco, "São Paulo", "SP");
    }

    @Test
    void naoPermiteCadastrarAdmin() { // RN11
        NegocioException e = assertThrows(NegocioException.class,
                () -> service.registrar(registro(Papel.ADMIN, null)));

        assertThat(e.getCodigo()).isEqualTo(CodigoErro.VALIDACAO);
        assertThat(e.getErros()).extracting(CampoErro::campo).containsExactly("papel");
        verifyNoInteractions(usuarios);
    }

    @Test
    void doadorPrecisaDeEndereco() {
        NegocioException e = assertThrows(NegocioException.class,
                () -> service.registrar(registro(Papel.DOADOR, "  ")));

        assertThat(e.getErros()).extracting(CampoErro::campo).containsExactly("endereco");
    }

    @Test
    void emailRepetidoDa409() {
        when(usuarios.existsByEmail("contato@teste.com")).thenReturn(true);

        NegocioException e = assertThrows(NegocioException.class,
                () -> service.registrar(registro(Papel.DOADOR, "Rua X")));

        assertThat(e.getCodigo()).isEqualTo(CodigoErro.EMAIL_EM_USO);
        verify(usuarios, never()).saveAndFlush(any());
    }

    @Test
    void receptorTemEnderecoDescartadoESenhaCriptografada() {
        when(usuarios.existsByEmail("contato@teste.com")).thenReturn(false);
        when(passwordEncoder.encode("Senha123")).thenReturn("hash-bcrypt");

        UsuarioResponse r = service.registrar(registro(Papel.RECEPTOR, "Rua que deve sumir"));

        assertThat(r.email()).isEqualTo("contato@teste.com"); // normalizado
        assertThat(r.endereco()).isNull();
        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).saveAndFlush(salvo.capture());
        assertThat(salvo.getValue().getSenhaHash()).isEqualTo("hash-bcrypt");
    }

    @Test
    void loginComSenhaErradaDa401() {
        Usuario u = usuario(1L, Papel.DOADOR);
        when(usuarios.findByEmail("u1@teste.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("Errada123", "hash")).thenReturn(false);

        NegocioException e = assertThrows(NegocioException.class,
                () -> service.login(new LoginRequest(" U1@teste.com", "Errada123")));

        assertThat(e.getCodigo()).isEqualTo(CodigoErro.CREDENCIAIS_INVALIDAS);
        verifyNoInteractions(tokenService);
    }

    @Test
    void loginDeEmailInexistenteDaOMesmo401() {
        when(usuarios.findByEmail("ninguem@teste.com")).thenReturn(Optional.empty());

        NegocioException e = assertThrows(NegocioException.class,
                () -> service.login(new LoginRequest("ninguem@teste.com", "Senha123")));

        assertThat(e.getCodigo()).isEqualTo(CodigoErro.CREDENCIAIS_INVALIDAS);
    }

    @Test
    void loginDeUsuarioInativoDaOMesmo401() {
        Usuario u = usuario(1L, Papel.DOADOR);
        definir(u, "ativo", false);
        when(usuarios.findByEmail("u1@teste.com")).thenReturn(Optional.of(u));

        NegocioException e = assertThrows(NegocioException.class,
                () -> service.login(new LoginRequest("u1@teste.com", "Senha123")));

        assertThat(e.getCodigo()).isEqualTo(CodigoErro.CREDENCIAIS_INVALIDAS);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void loginValidoDevolveOToken() {
        Usuario u = usuario(1L, Papel.DOADOR);
        Instant expira = Instant.parse("2026-11-12T05:00:00Z");
        when(usuarios.findByEmail("u1@teste.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("Senha123", "hash")).thenReturn(true);
        when(tokenService.emitir(u)).thenReturn(new TokenService.TokenEmitido("token-gerado", expira));

        LoginResponse r = service.login(new LoginRequest("u1@teste.com", "Senha123"));

        assertThat(r.token()).isEqualTo("token-gerado");
        assertThat(r.tipo()).isEqualTo("Bearer");
        assertThat(r.expiraEm()).isEqualTo(expira);
        assertThat(r.usuario().papel()).isEqualTo(Papel.DOADOR);
    }
}