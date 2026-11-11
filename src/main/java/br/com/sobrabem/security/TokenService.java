package br.com.sobrabem.security;

import br.com.sobrabem.domain.entity.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class TokenService {

    public record TokenEmitido(String token, Instant expiraEm) {
    }

    private final JwtEncoder encoder;
    private final Duration validade;

    public TokenService(JwtEncoder encoder, @Value("${sobrabem.jwt.validade:2h}") Duration validade) {
        this.encoder = encoder;
        this.validade = validade;
    }

    public TokenEmitido emitir(Usuario usuario) {
        Instant agora = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiraEm = agora.plus(validade);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("sobrabem-java")
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .subject(usuario.getId().toString())
                .claim("email", usuario.getEmail())
                .claim("papel", usuario.getPapel().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenEmitido(token, expiraEm);
    }
}