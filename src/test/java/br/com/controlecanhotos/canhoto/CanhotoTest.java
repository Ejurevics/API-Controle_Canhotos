package br.com.controlecanhotos.canhoto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import br.com.controlecanhotos.carga.Carga;
import br.com.controlecanhotos.carga.StatusCarga;
import br.com.controlecanhotos.coleta.Coleta;
import br.com.controlecanhotos.historico.HistoricoCanhoto;
import br.com.controlecanhotos.shared.exception.ConfirmacaoNecessariaException;
import br.com.controlecanhotos.shared.exception.RegraNegocioException;

class CanhotoTest {

	private static final LocalDate RECEBIMENTO = LocalDate.of(2026, 10, 5);

	private Carga novaCarga() {
		Coleta coleta = new Coleta("Cliente A", "11111111000111", LocalDate.of(2026, 10, 2));
		return coleta.adicionarCarga("11111111000111", (short) 1, 1234, BigDecimal.TEN, "Campinas", "SP");
	}

	private Canhoto novoCanhoto() {
		Carga carga = novaCarga();
		carga.alterarStatus(StatusCarga.COLETADA, null);
		carga.alterarStatus(StatusCarga.EM_TRANSITO, null);
		carga.alterarStatus(StatusCarga.ENTREGUE, null);
		return carga.getCanhoto();
	}

	@Test
	void nascePendenteSemDataDeRecebimento() {
		Canhoto canhoto = novaCarga().getCanhoto();

		assertThat(canhoto.getStatus()).isEqualTo(StatusCanhoto.PENDENTE);
		assertThat(canhoto.getDataRecebimento()).isNull();
	}

	@Test
	void registrarRecebimentoComCargaNaoEntregueLancaExcecaoENaoMudaNada() {
		Carga carga = novaCarga();
		carga.alterarStatus(StatusCarga.COLETADA, null);
		Canhoto canhoto = carga.getCanhoto();

		assertThatThrownBy(() -> canhoto.registrarRecebimento(RECEBIMENTO, false))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining("ENTREGUE")
				.hasMessageContaining("COLETADA");
		assertThat(canhoto.getStatus()).isEqualTo(StatusCanhoto.PENDENTE);
		assertThat(canhoto.getDataRecebimento()).isNull();
	}

	@Test
	void registrarDivergenciaComCargaNaoEntregueLancaExcecaoENaoMudaNada() {
		Canhoto canhoto = novaCarga().getCanhoto();

		assertThatThrownBy(() -> canhoto.registrarDivergencia(RECEBIMENTO, "Assinatura ilegível", false))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining("ENTREGUE");
		assertThat(canhoto.getStatus()).isEqualTo(StatusCanhoto.PENDENTE);
		assertThat(canhoto.getObservacao()).isNull();
	}

	@Test
	void registrarRecebimentoMarcaRecebidoComData() {
		Canhoto canhoto = novoCanhoto();

		canhoto.registrarRecebimento(RECEBIMENTO, false);

		assertThat(canhoto.getStatus()).isEqualTo(StatusCanhoto.RECEBIDO);
		assertThat(canhoto.getDataRecebimento()).isEqualTo(RECEBIMENTO);
	}

	@Test
	void registrarRecebimentoSemDataLancaExcecaoENaoMudaNada() {
		Canhoto canhoto = novoCanhoto();

		assertThatThrownBy(() -> canhoto.registrarRecebimento(null, false))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining("Data de recebimento");
		assertThat(canhoto.getStatus()).isEqualTo(StatusCanhoto.PENDENTE);
	}

	@Test
	void registrarDivergenciaMarcaDivergenteComDataEObservacao() {
		Canhoto canhoto = novoCanhoto();

		canhoto.registrarDivergencia(RECEBIMENTO, "Assinatura ilegível", false);

		assertThat(canhoto.getStatus()).isEqualTo(StatusCanhoto.DIVERGENTE);
		assertThat(canhoto.getDataRecebimento()).isEqualTo(RECEBIMENTO);
		assertThat(canhoto.getObservacao()).isEqualTo("Assinatura ilegível");
	}

	@Test
	void registrarDivergenciaSemDataLancaExcecaoENaoMudaNada() {
		Canhoto canhoto = novoCanhoto();

		assertThatThrownBy(() -> canhoto.registrarDivergencia(null, "Assinatura ilegível", false))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining("Data de recebimento");
		assertThat(canhoto.getStatus()).isEqualTo(StatusCanhoto.PENDENTE);
		assertThat(canhoto.getObservacao()).isNull();
	}

	@Test
	void registrarDivergenciaSemObservacaoLancaExcecaoENaoMudaNada() {
		Canhoto canhoto = novoCanhoto();

		assertThatThrownBy(() -> canhoto.registrarDivergencia(RECEBIMENTO, " ", false))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining("Observação");
		assertThat(canhoto.getStatus()).isEqualTo(StatusCanhoto.PENDENTE);
		assertThat(canhoto.getDataRecebimento()).isNull();
	}

	@Test
	void nasceComHistoricoInicialPendente() {
		Canhoto canhoto = novaCarga().getCanhoto();

		assertThat(canhoto.getHistorico())
				.extracting(HistoricoCanhoto::getStatusAnterior, HistoricoCanhoto::getStatusNovo)
				.containsExactly(tuple(null, StatusCanhoto.PENDENTE));
	}

	@Test
	void cadaRegistroEntraNoHistoricoComSeusDados() {
		Canhoto canhoto = novoCanhoto();

		canhoto.registrarDivergencia(RECEBIMENTO, "Assinatura ilegível", false);
		canhoto.registrarRecebimento(RECEBIMENTO.plusDays(2), false);

		assertThat(canhoto.getHistorico())
				.extracting(HistoricoCanhoto::getStatusAnterior, HistoricoCanhoto::getStatusNovo,
						HistoricoCanhoto::getDataRecebimento, HistoricoCanhoto::getObservacao)
				.containsExactly(
						tuple(null, StatusCanhoto.PENDENTE, null, null),
						tuple(StatusCanhoto.PENDENTE, StatusCanhoto.DIVERGENTE, RECEBIMENTO, "Assinatura ilegível"),
						tuple(StatusCanhoto.DIVERGENTE, StatusCanhoto.RECEBIDO, RECEBIMENTO.plusDays(2), null));
	}

	@Test
	void recebimentoAposDivergenciaLimpaObservacaoAtual() {
		Canhoto canhoto = novoCanhoto();
		canhoto.registrarDivergencia(RECEBIMENTO, "Assinatura ilegível", false);

		canhoto.registrarRecebimento(RECEBIMENTO.plusDays(2), false);

		assertThat(canhoto.getStatus()).isEqualTo(StatusCanhoto.RECEBIDO);
		assertThat(canhoto.getObservacao()).isNull();
	}

	@Test
	void registroRecusadoNaoEntraNoHistorico() {
		Canhoto canhoto = novoCanhoto();

		assertThatThrownBy(() -> canhoto.registrarDivergencia(RECEBIMENTO, " ", false))
				.isInstanceOf(RegraNegocioException.class);

		assertThat(canhoto.getHistorico()).hasSize(1);
	}

	@Test
	void registrarRecebimentoComDataDeHojeNaoPedeConfirmacao() {
		Canhoto canhoto = novoCanhoto();

		canhoto.registrarRecebimento(LocalDate.now(), false);

		assertThat(canhoto.getStatus()).isEqualTo(StatusCanhoto.RECEBIDO);
	}

	@Test
	void registrarRecebimentoComDataFuturaSemConfirmacaoPedeConfirmacaoENaoMudaNada() {
		Canhoto canhoto = novoCanhoto();
		LocalDate amanha = LocalDate.now().plusDays(1);

		assertThatThrownBy(() -> canhoto.registrarRecebimento(amanha, false))
				.isInstanceOf(ConfirmacaoNecessariaException.class)
				.hasMessageContaining("posterior à data atual");
		assertThat(canhoto.getStatus()).isEqualTo(StatusCanhoto.PENDENTE);
		assertThat(canhoto.getDataRecebimento()).isNull();
	}

	@Test
	void registrarRecebimentoComDataFuturaConfirmadaMarcaRecebido() {
		Canhoto canhoto = novoCanhoto();
		LocalDate amanha = LocalDate.now().plusDays(1);

		canhoto.registrarRecebimento(amanha, true);

		assertThat(canhoto.getStatus()).isEqualTo(StatusCanhoto.RECEBIDO);
		assertThat(canhoto.getDataRecebimento()).isEqualTo(amanha);
	}

	@Test
	void registrarDivergenciaComDataFuturaSemConfirmacaoPedeConfirmacaoENaoMudaNada() {
		Canhoto canhoto = novoCanhoto();
		LocalDate amanha = LocalDate.now().plusDays(1);

		assertThatThrownBy(() -> canhoto.registrarDivergencia(amanha, "Assinatura ilegível", false))
				.isInstanceOf(ConfirmacaoNecessariaException.class);
		assertThat(canhoto.getStatus()).isEqualTo(StatusCanhoto.PENDENTE);
		assertThat(canhoto.getObservacao()).isNull();
	}

	@Test
	void registrarDivergenciaComDataFuturaConfirmadaMarcaDivergente() {
		Canhoto canhoto = novoCanhoto();
		LocalDate amanha = LocalDate.now().plusDays(1);

		canhoto.registrarDivergencia(amanha, "Assinatura ilegível", true);

		assertThat(canhoto.getStatus()).isEqualTo(StatusCanhoto.DIVERGENTE);
		assertThat(canhoto.getDataRecebimento()).isEqualTo(amanha);
	}
}
