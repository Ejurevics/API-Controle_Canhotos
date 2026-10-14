package br.com.controlecanhotos;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Faz toda requisição do MockMvc sair autenticada como ADMIN, para os testes de regra de negócio não
 * repetirem o login. Autenticação e permissões por perfil são testadas em {@code SegurancaTest}, sem isto.
 */
@TestConfiguration(proxyBeanMethods = false)
public class AutenticadoComoAdmin {

	@Bean
	MockMvcBuilderCustomizer requisicoesComoAdmin() {
		return builder -> builder.defaultRequest(get("/")
				.with(jwt().jwt(j -> j.subject("admin-dos-testes"))
						.authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))));
	}
}
