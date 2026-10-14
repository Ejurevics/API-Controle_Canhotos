package br.com.controlecanhotos.seguranca;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração em {@code app.seguranca.*} (ver application.yml e README):
 * segredo e validade do JWT e dados do primeiro ADMIN.
 */
@ConfigurationProperties("app.seguranca")
public record PropriedadesSeguranca(Jwt jwt, AdminInicial adminInicial) {

	public record Jwt(String segredo, Duration expiracao) {
	}

	public record AdminInicial(String login, String nome, String senha) {
	}
}
