package br.com.controlecanhotos.seguranca;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.com.controlecanhotos.shared.exception.TentativasExcedidasException;

/**
 * Conta falhas de login por login e por IP em janelas fixas (ADR 0003). Ao atingir o limite, novas tentativas
 * respondem 429 até a janela acabar, sem nem conferir a senha. Login inexistente conta igual, para o bloqueio
 * não revelar quais logins existem.
 *
 * <p>Fica em memória: zera ao reiniciar e não é compartilhado entre instâncias, o que basta para uma instância.
 */
@Component
public class LimiteTentativasLogin {

	/** Acima disso, cada falha nova também descarta as janelas já vencidas, para o mapa não crescer sem fim. */
	private static final int TAMANHO_PARA_LIMPEZA = 10_000;

	private record Janela(int falhas, Instant inicio) {
	}

	private final Map<String, Janela> janelas = new ConcurrentHashMap<>();
	private final PropriedadesSeguranca.Login config;
	private final Clock relogio;

	@Autowired
	public LimiteTentativasLogin(PropriedadesSeguranca propriedades) {
		this(propriedades.login(), Clock.systemUTC());
	}

	LimiteTentativasLogin(PropriedadesSeguranca.Login config, Clock relogio) {
		this.config = config;
		this.relogio = relogio;
	}

	/** Lança {@link TentativasExcedidasException} se o login ou o IP atingiu o limite de falhas. */
	public void verificar(String login, String ip) {
		Instant agora = relogio.instant();
		Stream.of(espera(chaveLogin(login), config.maxFalhasPorLogin(), agora),
				espera(chaveIp(ip), config.maxFalhasPorIp(), agora))
				.flatMap(Optional::stream)
				.max(Comparator.naturalOrder())
				.ifPresent(espera -> {
					throw new TentativasExcedidasException(espera);
				});
	}

	public void registrarFalha(String login, String ip) {
		Instant agora = relogio.instant();
		incrementar(chaveLogin(login), agora);
		incrementar(chaveIp(ip), agora);
		if (janelas.size() > TAMANHO_PARA_LIMPEZA) {
			janelas.values().removeIf(janela -> vencida(janela, agora));
		}
	}

	/**
	 * Zera só o contador do login. O do IP continua, senão quem tem uma conta válida poderia entrar nela entre
	 * as tentativas para zerar o IP e testar senhas em outros logins.
	 */
	public void registrarSucesso(String login) {
		janelas.remove(chaveLogin(login));
	}

	private Optional<Duration> espera(String chave, int maxFalhas, Instant agora) {
		Janela janela = janelas.get(chave);
		if (janela == null || vencida(janela, agora) || janela.falhas() < maxFalhas) {
			return Optional.empty();
		}
		return Optional.of(Duration.between(agora, janela.inicio().plus(config.janela())));
	}

	private void incrementar(String chave, Instant agora) {
		janelas.compute(chave, (k, janela) -> janela == null || vencida(janela, agora)
				? new Janela(1, agora)
				: new Janela(janela.falhas() + 1, janela.inicio()));
	}

	private boolean vencida(Janela janela, Instant agora) {
		return !agora.isBefore(janela.inicio().plus(config.janela()));
	}

	private static String chaveLogin(String login) {
		return "login:" + login;
	}

	private static String chaveIp(String ip) {
		return "ip:" + ip;
	}
}
