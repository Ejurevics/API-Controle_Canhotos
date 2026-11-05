package br.com.controlecanhotos.seguranca;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Guarda por pouco tempo, por login, se o usuário está ativo e a versão da credencial (ADR 0005), para a checagem
 * do token não consultar o banco a cada requisição. Quem muda a credencial chama {@link #invalidar(String)}, e a
 * revogação continua imediata nesta instância; o prazo só limita o atraso de mudanças feitas por fora (outra
 * instância, alteração direta no banco).
 */
@Component
public class CacheCredenciais {

	/** O que a checagem do token precisa saber do usuário. */
	public record Credencial(boolean ativo, int versao) {
	}

	/** Acima disso, cada entrada nova também descarta as vencidas, para o mapa não crescer sem fim. */
	private static final int TAMANHO_PARA_LIMPEZA = 10_000;

	private record Entrada(Credencial credencial, Instant venceEm) {
	}

	private final Map<String, Entrada> entradas = new ConcurrentHashMap<>();
	private final Duration validade;
	private final Clock relogio;

	@Autowired
	public CacheCredenciais(PropriedadesSeguranca propriedades) {
		this(propriedades.jwt().cacheCredencial(), Clock.systemUTC());
	}

	CacheCredenciais(Duration validade, Clock relogio) {
		this.validade = validade == null ? Duration.ZERO : validade;
		this.relogio = relogio;
	}

	/**
	 * Devolve a credencial guardada ou a carrega com {@code carregar} (vazio se o usuário não existe, e aí nada
	 * é guardado). Com validade zero, sempre carrega.
	 */
	public Optional<Credencial> obter(String login, Function<String, Optional<Credencial>> carregar) {
		Instant agora = relogio.instant();
		Entrada entrada = entradas.get(login);
		if (entrada != null && agora.isBefore(entrada.venceEm())) {
			return Optional.of(entrada.credencial());
		}
		Optional<Credencial> carregada = carregar.apply(login);
		if (carregada.isPresent() && !validade.isZero()) {
			entradas.put(login, new Entrada(carregada.get(), agora.plus(validade)));
			if (entradas.size() > TAMANHO_PARA_LIMPEZA) {
				entradas.values().removeIf(e -> !agora.isBefore(e.venceEm()));
			}
		} else {
			entradas.remove(login);
		}
		return carregada;
	}

	/**
	 * Descarta a credencial guardada agora e, dentro de uma transação, de novo depois do commit: uma requisição
	 * que chegue entre a mudança e o commit ainda lê o valor antigo do banco e o guardaria de volta.
	 */
	public void invalidar(String login) {
		entradas.remove(login);
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					entradas.remove(login);
				}
			});
		}
	}
}
