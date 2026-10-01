package br.com.controlecanhotos.carga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import br.com.controlecanhotos.coleta.Coleta;
import br.com.controlecanhotos.historico.HistoricoStatus;
import br.com.controlecanhotos.shared.exception.RegraNegocioException;

class CargaTest {

	private Carga novaCarga() {
		Coleta coleta = new Coleta("Cliente A", "11111111000111", LocalDate.of(2026, 10, 2));
		return coleta.adicionarCarga("11111111000111", (short) 1, 1234, BigDecimal.TEN, "Campinas", "SP");
	}

	@Test
	void nasceAguardandoColetaComCanhotoPendenteEHistoricoInicial() {
		Carga carga = novaCarga();

		assertThat(carga.getStatus()).isEqualTo(StatusCarga.AGUARDANDO_COLETA);
		assertThat(carga.getCanhoto()).isNotNull();
		assertThat(carga.getHistorico()).hasSize(1);
		assertThat(carga.getHistorico().getFirst().getStatusAnterior()).isNull();
	}

	@Test
	void alterarStatusValidoRegistraHistorico() {
		Carga carga = novaCarga();

		carga.alterarStatus(StatusCarga.COLETADA, "Coletada");

		assertThat(carga.getStatus()).isEqualTo(StatusCarga.COLETADA);
		assertThat(carga.getHistorico()).hasSize(2);
		assertThat(carga.getHistorico().getLast().getMotivo()).isEqualTo("Coletada");
	}

	@Test
	void alterarStatusInvalidoLancaExcecaoENaoMudaNada() {
		Carga carga = novaCarga();

		assertThatThrownBy(() -> carga.alterarStatus(StatusCarga.ENTREGUE, null))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining("AGUARDANDO_COLETA -> ENTREGUE");
		assertThat(carga.getStatus()).isEqualTo(StatusCarga.AGUARDANDO_COLETA);
		assertThat(carga.getHistorico()).hasSize(1);
	}

	@Test
	void ocorrenciaSemMotivoLancaExcecao() {
		Carga carga = novaCarga();

		assertThatThrownBy(() -> carga.alterarStatus(StatusCarga.OCORRENCIA, " "))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining("Motivo");
		assertThat(carga.getStatus()).isEqualTo(StatusCarga.AGUARDANDO_COLETA);
	}

	@Test
	void cargaEntregueSoRetrocedePorOcorrencia() {
		Carga carga = novaCarga();
		carga.alterarStatus(StatusCarga.COLETADA, null);
		carga.alterarStatus(StatusCarga.EM_TRANSITO, null);
		carga.alterarStatus(StatusCarga.ENTREGUE, null);

		assertThatThrownBy(() -> carga.alterarStatus(StatusCarga.EM_TRANSITO, null))
				.isInstanceOf(RegraNegocioException.class);

		carga.alterarStatus(StatusCarga.OCORRENCIA, "Cliente recusou após descarga");
		carga.alterarStatus(StatusCarga.DEVOLVIDA, null);

		assertThat(carga.getHistorico()).extracting(HistoricoStatus::getStatusNovo).containsExactly(
				StatusCarga.AGUARDANDO_COLETA, StatusCarga.COLETADA, StatusCarga.EM_TRANSITO,
				StatusCarga.ENTREGUE, StatusCarga.OCORRENCIA, StatusCarga.DEVOLVIDA);
	}

	@Test
	void devolvidaEhFinal() {
		assertThat(StatusCarga.DEVOLVIDA.proximosPermitidos()).isEmpty();
	}

	@Test
	void coletadaNaoVaiDiretoParaDevolvida() {
		assertThat(StatusCarga.COLETADA.podeIrPara(StatusCarga.DEVOLVIDA)).isFalse();
	}
}
