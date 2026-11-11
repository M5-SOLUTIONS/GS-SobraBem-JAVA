package br.com.sobrabem.config;

import br.com.sobrabem.security.CustomAccessDeniedHandler;
import br.com.sobrabem.security.CustomEntryPoint;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
public class SecurityConfig {

    /** A mesma chave assina (no login) e valida (em toda requisição). */
    @Bean
    SecretKey jwtSecretKey(@Value("${sobrabem.jwt.secret}") String segredo) {
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET precisa ter pelo menos 32 caracteres (HS256)");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey chave) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave));
    }

    /** O BearerTokenAuthenticationFilter do resource server usa este decoder: confere assinatura e expiração. */
    @Bean
    JwtDecoder jwtDecoder(SecretKey chave) {
        return NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, CustomEntryPoint entryPoint,
                                            CustomAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs/**", "/error").permitAll()

                        // Usuários: /me para qualquer logado; o resto é do ADMIN
                        .requestMatchers("/api/usuarios/me").authenticated()
                        .requestMatchers("/api/usuarios/**").hasRole("ADMIN")

                        // Categorias: leitura para logados; escrita só ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/categorias/**").authenticated()
                        .requestMatchers("/api/categorias/**").hasRole("ADMIN")

                        // Doações (o "só o dono" fica no service)
                        .requestMatchers(HttpMethod.POST, "/api/doacoes").hasRole("DOADOR")
                        .requestMatchers(HttpMethod.GET, "/api/doacoes/minhas", "/api/doacoes/*/reservas").hasRole("DOADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/doacoes/*").hasRole("DOADOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/doacoes/*/cancelamento").hasRole("DOADOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/doacoes/*").hasAnyRole("DOADOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/doacoes/**").authenticated()

                        // Reservas
                        .requestMatchers(HttpMethod.POST, "/api/reservas").hasRole("RECEPTOR")
                        .requestMatchers(HttpMethod.GET, "/api/reservas/minhas").hasRole("RECEPTOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/reservas/*/cancelamento").hasRole("RECEPTOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/reservas/*/retirada").hasRole("DOADOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/reservas/*").hasRole("ADMIN")

                        // IA
                        .requestMatchers(HttpMethod.POST, "/api/ia/**").hasRole("DOADOR")

                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler));
        return http.build();
    }

    /** Claim "papel": "DOADOR" → authority ROLE_DOADOR, que o hasRole("DOADOR") confere. */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter papeis = new JwtGrantedAuthoritiesConverter();
        papeis.setAuthoritiesClaimName("papel");
        papeis.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(papeis);
        return conversor;
    }
}