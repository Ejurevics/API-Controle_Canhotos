package br.com.controlecanhotos.coleta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import br.com.controlecanhotos.Cte;
import br.com.controlecanhotos.carga.Carga;
import br.com.controlecanhotos.carga.StatusCarga;
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

	private Carga adicionarCarga(Coleta coleta, int numeroNotaFiscal) {
		return Cte.cargaComUmaNota(coleta, numeroNotaFiscal);
	}

	private void entregar(Carga carga) {
		carga.alterarStatus(StatusCarga.COLETADA, null);
		carga.alterarStatus(StatusCarga.EM_TRANSITO, null);
		carga.alterarStatus(StatusCarga.ENTREGUE, null);
	}

	private void devolver(Carga carga) {
		carga.alterarStatus(StatusCarga.COLETADA, null);
		carga.alterarStatus(StatusCarga.EM_TRANSITO, null);
		carga.alterarStatus(StatusCarga.DEVOLVIDA, null);
	}

	@Test
	void concluiQuandoTodasAsCargasForemEntregues() {
		Coleta coleta = novaColeta();
		entregar(adicionarCarga(coleta, 1001));
		entregar(adicionarCarga(coleta, 1002));
		coleta.alterarSituacao(SituacaoColeta.EM_ANDAMENTO);

		coleta.alterarSituacao(SituacaoColeta.CONCLUIDA);

		assertThat(coleta.getSituacao()).isEqualTo(SituacaoColeta.CONCLUIDA);
	}

	@Test
	void concluiComMistoDeEntregueEDevolvida() {
		Coleta coleta = novaColeta();
		entregar(adicionarCarga(coleta, 1001));
		devolver(adicionarCarga(coleta, 1002));
		coleta.alterarSituacao(SituacaoColeta.EM_ANDAMENTO);

		coleta.alterarSituacao(SituacaoColeta.CONCLUIDA);

		assertThat(coleta.getSituacao()).isEqualTo(SituacaoColeta.CONCLUIDA);
	}

	@Test
	void naoConcluiComCargaNaoFinalizadaEListaAsPendentes() {
		Coleta coleta = novaColeta();
		entregar(adicionarCarga(coleta, 1001));
		adicionarCarga(coleta, 1002);
		Carga emTransito = adicionarCarga(coleta, 1003);
		emTransito.alterarStatus(StatusCarga.COLETADA, null);
		emTransito.alterarStatus(StatusCarga.EM_TRANSITO, null);
		coleta.alterarSituacao(SituacaoColeta.EM_ANDAMENTO);

		assertThatThrownBy(() -> coleta.alterarSituacao(SituacaoColeta.CONCLUIDA))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining(Cte.chave(1002) + " (AGUARDANDO_COLETA)")
				.hasMessageContaining(Cte.chave(1003) + " (EM_TRANSITO)")
				.hasMessageNotContaining(Cte.chave(1001));
		assertThat(coleta.getSituacao()).isEqualTo(SituacaoColeta.EM_ANDAMENTO);
	}

	@Test
	void cargaEmOcorrenciaImpedeConclusao() {
		Coleta coleta = novaColeta();
		Carga carga = adicionarCarga(coleta, 1001);
		carga.alterarStatus(StatusCarga.OCORRENCIA, "Cliente ausente");
		coleta.alterarSituacao(SituacaoColeta.EM_ANDAMENTO);

		assertThatThrownBy(() -> coleta.alterarSituacao(SituacaoColeta.CONCLUIDA))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining(Cte.chave(1001) + " (OCORRENCIA)");
	}

	@Test
	void cancelarNaoExigeCargasFinalizadas() {
		Coleta coleta = novaColeta();
		adicionarCarga(coleta, 1001);
		coleta.alterarSituacao(SituacaoColeta.EM_ANDAMENTO);

		coleta.alterarSituacao(SituacaoColeta.CANCELADA);

		assertThat(coleta.getSituacao()).isEqualTo(SituacaoColeta.CANCELADA);
	}

	@ParameterizedTest
	@EnumSource(value = SituacaoColeta.class, names = { "CONCLUIDA", "CANCELADA" })
	void situacoesFinaisNaoTemProxima(SituacaoColeta final_) {
		assertThat(final_.proximasPermitidas()).isEmpty();
	}
}
