package br.com.controlecanhotos.carga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.controlecanhotos.Cte;
import br.com.controlecanhotos.canhoto.StatusCanhoto;
import br.com.controlecanhotos.coleta.Coleta;
import br.com.controlecanhotos.historico.HistoricoStatus;
import br.com.controlecanhotos.shared.exception.RegraNegocioException;

class CargaTest {

	private Carga novaCarga() {
		Coleta coleta = new Coleta("Cliente A", "11111111000111", LocalDate.of(2026, 10, 2));
		Carga carga = coleta.adicionarCarga(Cte.chave(1), "Campinas", "SP");
		carga.adicionarNotaFiscal("11111111000111", (short) 1, 1234, BigDecimal.TEN);
		return carga;
	}

	@Test
	void nasceAguardandoColetaComCanhotoPendenteEHistoricoInicial() {
		Carga carga = novaCarga();

		assertThat(carga.getStatus()).isEqualTo(StatusCarga.AGUARDANDO_COLETA);
		assertThat(carga.getNotasFiscais()).singleElement()
				.satisfies(nota -> assertThat(nota.getCanhoto().getStatus()).isEqualTo(StatusCanhoto.PENDENTE));
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

	@Test
	void cteAgrupaVariasNotasCadaUmaComCanhotoPendenteEPesoSomado() {
		Carga carga = novaCarga();

		carga.adicionarNotaFiscal("11111111000111", (short) 1, 1235, new BigDecimal("5.5"));

		assertThat(carga.getNotasFiscais()).hasSize(2);
		assertThat(carga.getNotasFiscais()).allSatisfy(
				nota -> assertThat(nota.getCanhoto().getStatus()).isEqualTo(StatusCanhoto.PENDENTE));
		assertThat(carga.getPesoTotalKg()).isEqualByComparingTo("15.5");
	}

	@Test
	void rejeitaMesmaNotaDuasVezesNoMesmoCte() {
		Carga carga = novaCarga();

		assertThatThrownBy(() -> carga.adicionarNotaFiscal("11111111000111", (short) 1, 1234, BigDecimal.ONE))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining("repetida no CT-e")
				.hasMessageContaining("11111111000111/1/1234");
		assertThat(carga.getNotasFiscais()).hasSize(1);
	}

	@Test
	void mesmoNumeroComSerieOuEmitenteDiferenteNoMesmoCteEhOutraNota() {
		Carga carga = novaCarga();

		carga.adicionarNotaFiscal("11111111000111", (short) 2, 1234, BigDecimal.ONE);
		carga.adicionarNotaFiscal("22222222000122", (short) 1, 1234, BigDecimal.ONE);

		assertThat(carga.getNotasFiscais()).hasSize(3);
	}

	@Test
	void mesmaNotaPodeEstarEmCtesDiferentes() {
		Coleta coleta = new Coleta("Cliente A", "11111111000111", LocalDate.of(2026, 10, 2));
		Carga primeira = coleta.adicionarCarga(Cte.chave(1), "Campinas", "SP");
		Carga segunda = coleta.adicionarCarga(Cte.chave(2), "Santos", "SP");

		primeira.adicionarNotaFiscal("11111111000111", (short) 1, 1234, BigDecimal.TEN);
		segunda.adicionarNotaFiscal("11111111000111", (short) 1, 1234, BigDecimal.TEN);

		assertThat(primeira.getNotasFiscais()).hasSize(1);
		assertThat(segunda.getNotasFiscais()).hasSize(1);
	}

	@Test
	void cteCriadoPeloSistemaNaoTemChaveProvisoria() {
		assertThat(novaCarga().isChaveProvisoria()).isFalse();
	}

	/** Simula uma carga migrada pela V3, que é o único jeito de existir chave provisória. */
	static Carga cargaMigrada() {
		Carga carga = new Coleta("Cliente A", "11111111000111", LocalDate.of(2026, 10, 2))
				.adicionarCarga("99" + "0".repeat(41) + "1", "Campinas", "SP");
		ReflectionTestUtils.setField(carga, "chaveProvisoria", true);
		return carga;
	}

	@Test
	void corrigeChaveProvisoriaQueViraDefinitiva() {
		Carga carga = cargaMigrada();
		String provisoria = carga.getChaveAcesso();

		carga.corrigirChaveAcesso(Cte.chave(7), "admin");

		assertThat(carga.getChaveAcesso()).isEqualTo(Cte.chave(7));
		assertThat(carga.isChaveProvisoria()).isFalse();
		assertThat(carga.getChaveProvisoriaAnterior()).isEqualTo(provisoria);
		assertThat(carga.getChaveCorrigidaPor()).isEqualTo("admin");
		assertThat(carga.getChaveCorrigidaEm()).isNotNull();
	}

	@Test
	void naoCorrigeChaveDefinitiva() {
		Carga carga = novaCarga();

		assertThatThrownBy(() -> carga.corrigirChaveAcesso(Cte.chave(7), "admin"))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining("já é definitiva");
		assertThat(carga.getChaveAcesso()).isEqualTo(Cte.chave(1));
	}
}
