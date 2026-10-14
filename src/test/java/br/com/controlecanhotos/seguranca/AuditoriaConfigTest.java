package br.com.controlecanhotos.seguranca;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class AuditoriaConfigTest {

	@AfterEach
	void limparContexto() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void usaOLoginDoToken() {
		Jwt jwt = Jwt.withTokenValue("t").header("alg", "HS256").subject("maria").build();
		SecurityContextHolder.getContext().setAuthentication(
				new JwtAuthenticationToken(jwt, AuthorityUtils.createAuthorityList("ROLE_OPERADOR")));

		assertThat(AuditoriaConfig.loginAutenticado()).contains("maria");
	}

	@Test
	void semAutenticacaoFicaVazio() {
		assertThat(AuditoriaConfig.loginAutenticado()).isEmpty();
	}

	@Test
	void anonimoFicaVazio() {
		SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("chave", "anonymous",
				List.copyOf(AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"))));

		assertThat(AuditoriaConfig.loginAutenticado()).isEmpty();
	}
}
