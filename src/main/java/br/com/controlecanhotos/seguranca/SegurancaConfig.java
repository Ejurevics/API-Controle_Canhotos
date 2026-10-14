package br.com.controlecanhotos.seguranca;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
@EnableConfigurationProperties(PropriedadesSeguranca.class)
public class SegurancaConfig {

	public static final String EMISSOR = "controle-canhotos";
	public static final String CLAIM_PERFIL = "perfil";
	public static final String CLAIM_VERSAO_CREDENCIAL = "ver";

	private static final int TAMANHO_MINIMO_SEGREDO = 32;

	@Bean
	SecurityFilterChain filtros(HttpSecurity http,
			@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) throws Exception {
		// 401 e 403 passam pelo ApiExceptionHandler, para saírem no mesmo ProblemDetail do resto da API.
		AuthenticationEntryPoint naoAutenticado = (req, res, ex) -> resolver.resolveException(req, res, null, ex);
		AccessDeniedHandler semPermissao = (req, res, ex) -> resolver.resolveException(req, res, null, ex);

		return http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(a -> a
						.requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
						.requestMatchers("/actuator/health", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
						.permitAll()
						.requestMatchers(HttpMethod.PATCH, "/cargas/*/chave-acesso").hasRole("ADMIN")
						.requestMatchers("/usuarios", "/usuarios/**").hasRole("ADMIN")
						.anyRequest().hasAnyRole("ADMIN", "OPERADOR"))
				.oauth2ResourceServer(o -> o
						.jwt(j -> j.jwtAuthenticationConverter(conversorDePerfil()))
						.authenticationEntryPoint(naoAutenticado)
						.accessDeniedHandler(semPermissao))
				.exceptionHandling(e -> e.authenticationEntryPoint(naoAutenticado).accessDeniedHandler(semPermissao))
				.build();
	}

	/** O perfil vai no claim {@value #CLAIM_PERFIL} e vira a autoridade {@code ROLE_<perfil>}. */
	private static JwtAuthenticationConverter conversorDePerfil() {
		JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
		autoridades.setAuthoritiesClaimName(CLAIM_PERFIL);
		autoridades.setAuthorityPrefix("ROLE_");
		JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
		conversor.setJwtGrantedAuthoritiesConverter(autoridades);
		return conversor;
	}

	@Bean
	SecretKey chaveJwt(PropriedadesSeguranca propriedades) {
		String segredo = propriedades.jwt().segredo();
		if (segredo == null || segredo.getBytes(StandardCharsets.UTF_8).length < TAMANHO_MINIMO_SEGREDO) {
			throw new IllegalStateException("app.seguranca.jwt.segredo (JWT_SECRET) deve ter pelo menos "
					+ TAMANHO_MINIMO_SEGREDO + " bytes");
		}
		return new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	@Bean
	JwtDecoder jwtDecoder(SecretKey chaveJwt, ValidadorCredencialAtual credencialAtual) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chaveJwt).macAlgorithm(MacAlgorithm.HS256).build();
		OAuth2TokenValidator<Jwt> padrao = JwtValidators.createDefaultWithIssuer(EMISSOR);
		// Só consulta o banco se o token já passou nas validações de data e emissor.
		decoder.setJwtValidator(jwt -> {
			OAuth2TokenValidatorResult resultado = padrao.validate(jwt);
			return resultado.hasErrors() ? resultado : credencialAtual.validate(jwt);
		});
		return decoder;
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKey chaveJwt) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(chaveJwt));
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
