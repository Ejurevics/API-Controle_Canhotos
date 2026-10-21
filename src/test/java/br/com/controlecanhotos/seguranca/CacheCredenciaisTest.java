package br.com.controlecanhotos.seguranca;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import br.com.controlecanhotos.RelogioManual;
import br.com.controlecanhotos.seguranca.CacheCredenciais.Credencial;

class CacheCredenciaisTest {

	private final RelogioManual relogio = new RelogioManual();
	private final CacheCredenciais cache = new CacheCredenciais(Duration.ofSeconds(30), relogio);

	/** Conta as idas ao "banco" e devolve a credencial atual. */
	private final AtomicInteger consultas = new AtomicInteger();
	private Credencial noBanco = new Credencial(true, 0);
	private final Function<String, Optional<Credencial>> banco = login -> {
		consultas.incrementAndGet();
		return Optional.ofNullable(noBanco);
	};

	@Test
	void reaproveitaACredencialDentroDoPrazo() {
		cache.obter("maria", banco);
		relogio.avancar(Duration.ofSeconds(29));

		assertThat(cache.obter("maria", banco)).contains(new Credencial(true, 0));
		assertThat(consultas).hasValue(1);
	}

	@Test
	void consultaDeNovoQuandoOPrazoVence() {
		cache.obter("maria", banco);
		noBanco = new Credencial(true, 1);
		relogio.avancar(Duration.ofSeconds(30));

		assertThat(cache.obter("maria", banco)).contains(new Credencial(true, 1));
		assertThat(consultas).hasValue(2);
	}

	@Test
	void invalidarFazAProximaChecagemIrAoBanco() {
		cache.obter("maria", banco);
		noBanco = new Credencial(false, 1);

		cache.invalidar("maria");

		assertThat(cache.obter("maria", banco)).contains(new Credencial(false, 1));
		assertThat(consultas).hasValue(2);
	}

	@Test
	void usuarioInexistenteNaoFicaGuardado() {
		noBanco = null;

		assertThat(cache.obter("ninguem", banco)).isEmpty();
		assertThat(cache.obter("ninguem", banco)).isEmpty();
		assertThat(consultas).hasValue(2);
	}

	@Test
	void comPrazoZeroSempreConsulta() {
		CacheCredenciais desligado = new CacheCredenciais(Duration.ZERO, relogio);

		desligado.obter("maria", banco);
		desligado.obter("maria", banco);

		assertThat(consultas).hasValue(2);
	}

	@Test
	void dentroDeTransacaoDescartaDeNovoDepoisDoCommit() {
		TransactionSynchronizationManager.initSynchronization();
		try {
			cache.invalidar("maria");
			// Uma requisição entre a mudança e o commit ainda lê o valor antigo e o guarda.
			cache.obter("maria", banco);

			TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
		} finally {
			TransactionSynchronizationManager.clearSynchronization();
		}

		noBanco = new Credencial(false, 1);
		assertThat(cache.obter("maria", banco)).contains(new Credencial(false, 1));
		assertThat(consultas).hasValue(2);
	}
}
