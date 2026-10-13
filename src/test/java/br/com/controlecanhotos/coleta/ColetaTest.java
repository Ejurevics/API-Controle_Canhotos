package br.com.controlecanhotos.coleta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import br.com.controlecanhotos.shared.exception.RegraNegocioException;

class ColetaTest {

	private Coleta novaColeta() {
		return new Coleta("Cliente A", "11111111000111", LocalDate.of(2026, 10, 2));
	}

	@Test
	void nasceProgramada() {
		assertThat(novaColeta().getSituacao()).isEqualTo(SituacaoColeta.PROGRAMADA);
	}

	@Test
	void seguePeloFluxoAteConcluida() {
		Coleta coleta = novaColeta();

		coleta.alterarSituacao(SituacaoColeta.EM_ANDAMENTO);
		coleta.alterarSituacao(SituacaoColeta.CONCLUIDA);

		assertThat(coleta.getSituacao()).isEqualTo(SituacaoColeta.CONCLUIDA);
	}

	@Test
	void podeSerCanceladaAntesEDuranteAExecucao() {
		Coleta programada = novaColeta();
		programada.alterarSituacao(SituacaoColeta.CANCELADA);

		Coleta emAndamento = novaColeta();
		emAndamento.alterarSituacao(SituacaoColeta.EM_ANDAMENTO);
		emAndamento.alterarSituacao(SituacaoColeta.CANCELADA);

		assertThat(programada.getSituacao()).isEqualTo(SituacaoColeta.CANCELADA);
		assertThat(emAndamento.getSituacao()).isEqualTo(SituacaoColeta.CANCELADA);
	}

	@Test
	void naoPulaDeProgramadaParaConcluida() {
		Coleta coleta = novaColeta();

		assertThatThrownBy(() -> coleta.alterarSituacao(SituacaoColeta.CONCLUIDA))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining("PROGRAMADA -> CONCLUIDA");
		assertThat(coleta.getSituacao()).isEqualTo(SituacaoColeta.PROGRAMADA);
	}

	@ParameterizedTest
	@EnumSource(value = SituacaoColeta.class, names = { "CONCLUIDA", "CANCELADA" })
	void situacoesFinaisNaoTemProxima(SituacaoColeta final_) {
		assertThat(final_.proximasPermitidas()).isEmpty();
	}
}
