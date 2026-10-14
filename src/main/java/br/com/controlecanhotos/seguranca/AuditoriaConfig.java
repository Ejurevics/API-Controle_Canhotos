package br.com.controlecanhotos.seguranca;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Preenche os campos {@code @CreatedBy} dos históricos com o login do usuário autenticado (o {@code sub} do
 * token). Fora de uma requisição autenticada (por exemplo, na subida), o campo fica nulo.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "usuarioLogado")
public class AuditoriaConfig {

	@Bean
	AuditorAware<String> usuarioLogado() {
		return AuditoriaConfig::loginAutenticado;
	}

	static Optional<String> loginAutenticado() {
		Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
		if (autenticacao == null || !autenticacao.isAuthenticated()
				|| autenticacao instanceof AnonymousAuthenticationToken) {
			return Optional.empty();
		}
		return Optional.of(autenticacao.getName());
	}
}
