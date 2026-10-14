package br.com.controlecanhotos.seguranca;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração em {@code app.seguranca.*} (ver application.yml e README):
 * segredo e validade do JWT, dados do primeiro ADMIN e limite de tentativas de login.
 */
@ConfigurationProperties("app.seguranca")
public record PropriedadesSeguranca(Jwt jwt, AdminInicial adminInicial, Login login) {

	public record Jwt(String segredo, Duration expiracao) {
	}

	public record AdminInicial(String login, String nome, String senha) {
	}

	/** Falhas permitidas por login e por IP dentro da janela; ao atingir o limite, bloqueia até ela acabar. */
	public record Login(int maxFalhasPorLogin, int maxFalhasPorIp, Duration janela) {
	}
}
