package br.com.controlecanhotos.canhoto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.controlecanhotos.Cte;
import br.com.controlecanhotos.carga.Carga;
import br.com.controlecanhotos.carga.StatusCarga;
import br.com.controlecanhotos.coleta.Coleta;
import br.com.controlecanhotos.historico.HistoricoCanhoto;
import br.com.controlecanhotos.shared.exception.ConfirmacaoNecessariaException;
import br.com.controlecanhotos.shared.exception.OperacaoNaoPermitidaException;
import br.com.controlecanhotos.shared.exception.RecursoNaoEncontradoException;
import br.com.controlecanhotos.shared.exception.RegraNegocioException;

class CanhotoTest {

	private static final LocalDate RECEBIMENTO = LocalDate.of(2026, 10, 5);

	// 01:30 UTC do dia 14 ainda é 22:30 do dia 13 em Brasília
	private static final Clock SERVIDOR_UTC_JA_NO_DIA_SEGUINTE = Clock.fixed(
			Instant.parse("2026-10-14T01:30:00Z"), ZoneOffset.UTC);
	private static final LocalDate HOJE_EM_BRASILIA = LocalDate.of(2026, 10, 13);
	private static final LocalDate AMANHA_EM_BRASILIA = LocalDate.of(2026, 10, 14);

	private Carga novaCarga() {
		Coleta coleta = new Coleta("Cliente A", "11111111000111", LocalDate.of(2026, 10, 2));
		Carga carga = coleta.adicionarCarga(Cte.chave(1), "Campinas", "SP");
		carga.adicionarNotaFiscal("11111111000111", (short) 1, 1234, BigDecimal.TEN);
		return carga;
	}

	private Canhoto novoCanhoto() {
		Carga carga = novaCarga();
		carga.alterarStatus(StatusCarga.COLETADA, null);
		carga.alterarStatus(StatusCarga.EM_TRANSITO, null);
		carga.alterarStatus(StatusCarga.ENTREGUE, null);
		return carga.getNotasFiscais().getFirst().getCanhoto();
	}

	@Test
	void nascePendenteSemDataDeRecebimento() {
		Canhoto canhoto = novaCarga().getNotasFiscais().getFirst().getCanhoto();

		assertThat(canhoto.getStatus()).isEqualTo(StatusCanhoto.PENDENTE);
		assertThat(canhoto.getDataRecebimento()).isNull();
	}

	@Test
	void registrarRecebimentoComCargaNaoEntregueLancaExcecaoENaoMudaNada() {
		Carga carga = novaCarga();
		carga.alterarStatus(StatusCarga.COLETADA, null);
		Canhoto canhoto = carga.getNotasFiscais().getFirst().getCanhoto();

		assertThatThrownBy(() -> canhoto.registrarRecebimento(RECEBIMENTO, false))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining("ENTREGUE")
				.hasMessageContaining("COLETADA");
		assertThat(canhoto.getStatus()).isEqualTo(StatusCanhoto.PENDENTE);
		assertThat(canhoto.getDataRecebimento()).isNull();
	}

	@Test
	void registrarDivergenciaComCargaNaoEntregueLancaExcecaoENaoMudaNada() {
		Canhoto canhoto = novaCarga().getNotasFiscais().getFirst().getCanhoto();

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
		Canhoto canhoto = novaCarga().getNotasFiscais().getFirst().getCanhoto();

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
	void registrarRecebimentoComDataDeHojeEmBrasiliaNaoPedeConfirmacao() {
		Canhoto canhoto = novoCanhoto();

		canhoto.registrarRecebimento(HOJE_EM_BRASILIA, false, SERVIDOR_UTC_JA_NO_DIA_SEGUINTE);

		assertThat(canhoto.getStatus()).isEqualTo(StatusCanhoto.RECEBIDO);
	}

	@Test
	void registrarRecebimentoComDataFuturaEmBrasiliaPedeConfirmacaoENaoMudaNada() {
		Canhoto canhoto = novoCanhoto();

		assertThatThrownBy(() -> canhoto.registrarRecebimento(AMANHA_EM_BRASILIA, false,
				SERVIDOR_UTC_JA_NO_DIA_SEGUINTE))
				.isInstanceOf(ConfirmacaoNecessariaException.class)
				.hasMessageContaining("posterior à data atual");
		assertThat(canhoto.getStatus()).isEqualTo(StatusCanhoto.PENDENTE);
		assertThat(canhoto.getDataRecebimento()).isNull();
	}

	@Test
	void registrarRecebimentoComDataFuturaConfirmadaMarcaRecebido() {
		Canhoto canhoto = novoCanhoto();

		canhoto.registrarRecebimento(AMANHA_EM_BRASILIA, true, SERVIDOR_UTC_JA_NO_DIA_SEGUINTE);

		assertThat(canhoto.getStatus()).isEqualTo(StatusCanhoto.RECEBIDO);
		assertThat(canhoto.getDataRecebimento()).isEqualTo(AMANHA_EM_BRASILIA);
	}

	@Test
	void registrarDivergenciaComDataFuturaEmBrasiliaPedeConfirmacaoENaoMudaNada() {
		Canhoto canhoto = novoCanhoto();

		assertThatThrownBy(() -> canhoto.registrarDivergencia(AMANHA_EM_BRASILIA, "Assinatura ilegível",
				false, SERVIDOR_UTC_JA_NO_DIA_SEGUINTE))
				.isInstanceOf(ConfirmacaoNecessariaException.class);
		assertThat(canhoto.getStatus()).isEqualTo(StatusCanhoto.PENDENTE);
		assertThat(canhoto.getObservacao()).isNull();
	}

	@Test
	void registrarDivergenciaComDataFuturaConfirmadaMarcaDivergente() {
		Canhoto canhoto = novoCanhoto();

		canhoto.registrarDivergencia(AMANHA_EM_BRASILIA, "Assinatura ilegível", true,
				SERVIDOR_UTC_JA_NO_DIA_SEGUINTE);

		assertThat(canhoto.getStatus()).isEqualTo(StatusCanhoto.DIVERGENTE);
		assertThat(canhoto.getDataRecebimento()).isEqualTo(AMANHA_EM_BRASILIA);
	}

	@Test
	void historicoMarcaRegistroFeitoComDataFuturaConfirmada() {
		Canhoto canhoto = novoCanhoto();

		canhoto.registrarRecebimento(AMANHA_EM_BRASILIA, true, SERVIDOR_UTC_JA_NO_DIA_SEGUINTE);

		assertThat(canhoto.getHistorico())
				.extracting(HistoricoCanhoto::isDataFuturaConfirmada)
				.containsExactly(false, true);
	}

	@Test
	void historicoNaoMarcaConfirmacaoQuandoDataNaoEraFutura() {
		Canhoto canhoto = novoCanhoto();

		canhoto.registrarDivergencia(HOJE_EM_BRASILIA, "Assinatura ilegível", true,
				SERVIDOR_UTC_JA_NO_DIA_SEGUINTE);

		assertThat(canhoto.getHistorico().getLast().isDataFuturaConfirmada()).isFalse();
	}

	private static String sha(int n) {
		return "%064d".formatted(n);
	}

	@Test
	void anexaComprovanteComCargaEntregue() {
		Canhoto canhoto = novoCanhoto();

		AnexoCanhoto anexo = canhoto.anexar("canhoto.jpg", TipoAnexo.JPEG, 1024, sha(1));

		assertThat(canhoto.getAnexosAtivos()).containsExactly(anexo);
		assertThat(anexo.getCanhoto()).isSameAs(canhoto);
		assertThat(anexo.getNomeArquivo()).isEqualTo("canhoto.jpg");
		assertThat(anexo.getTipo()).isEqualTo(TipoAnexo.JPEG);
		assertThat(anexo.getTamanhoBytes()).isEqualTo(1024);
		assertThat(anexo.getSha256()).isEqualTo(sha(1));
		assertThat(canhoto.getStatus()).as("anexar não registra o canhoto").isEqualTo(StatusCanhoto.PENDENTE);
	}

	@Test
	void anexarComCargaNaoEntregueLancaExcecao() {
		Carga carga = novaCarga();
		carga.alterarStatus(StatusCarga.COLETADA, null);
		Canhoto canhoto = carga.getNotasFiscais().getFirst().getCanhoto();

		assertThatThrownBy(() -> canhoto.anexar("canhoto.jpg", TipoAnexo.JPEG, 1024, sha(1)))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("Comprovante só pode ser anexado com a carga ENTREGUE (status atual: COLETADA)");
		assertThat(canhoto.getAnexosAtivos()).isEmpty();
	}

	@Test
	void anexarMesmoArquivoDuasVezesLancaExcecao() {
		Canhoto canhoto = novoCanhoto();
		canhoto.anexar("frente.jpg", TipoAnexo.JPEG, 1024, sha(1));

		assertThatThrownBy(() -> canhoto.anexar("copia.jpg", TipoAnexo.JPEG, 1024, sha(1)))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageStartingWith("Este arquivo já está anexado ao canhoto");
		assertThat(canhoto.getAnexosAtivos()).hasSize(1);
	}

	@Test
	void anexarAlemDoMaximoLancaExcecao() {
		Canhoto canhoto = novoCanhoto();
		for (int i = 0; i < Canhoto.MAXIMO_ANEXOS; i++) {
			canhoto.anexar("foto" + i + ".jpg", TipoAnexo.JPEG, 1024, sha(i));
		}

		assertThatThrownBy(() -> canhoto.anexar("mais.jpg", TipoAnexo.JPEG, 1024, sha(99)))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("O canhoto já tem 10 anexos; remova um para enviar outro");
		assertThat(canhoto.getAnexosAtivos()).hasSize(Canhoto.MAXIMO_ANEXOS);
	}

	/** Sem banco nem auditoria no teste de unidade: id e autor vêm à mão. */
	private static AnexoCanhoto anexarComo(Canhoto canhoto, long id, String login, int sha) {
		AnexoCanhoto anexo = canhoto.anexar("foto" + id + ".jpg", TipoAnexo.JPEG, 1024, sha(sha));
		ReflectionTestUtils.setField(anexo, "id", id);
		ReflectionTestUtils.setField(anexo, "enviadoPor", login);
		return anexo;
	}

	@Test
	void quemEnviouRemoveOAnexoSemApagar() {
		Canhoto canhoto = novoCanhoto();
		AnexoCanhoto anexo = anexarComo(canhoto, 1L, "maria", 1);
		AnexoCanhoto outro = anexarComo(canhoto, 2L, "maria", 2);

		canhoto.removerAnexo(1L, "maria", false, "  Foto do canhoto errado  ");

		assertThat(anexo.isAtivo()).isFalse();
		assertThat(anexo.getRemovidoEm()).isNotNull();
		assertThat(anexo.getRemovidoPor()).isEqualTo("maria");
		assertThat(anexo.getMotivoRemocao()).isEqualTo("Foto do canhoto errado");
		assertThat(canhoto.getAnexosAtivos()).containsExactly(outro);
		assertThat(canhoto.buscarAnexoAtivo(1L)).isEmpty();
		assertThat(canhoto.buscarAnexoAtivo(2L)).contains(outro);
	}

	@Test
	void anexoRemovidoNaoContaNoLimiteNemNaDuplicata() {
		Canhoto canhoto = novoCanhoto();
		for (int i = 1; i <= Canhoto.MAXIMO_ANEXOS; i++) {
			anexarComo(canhoto, i, "maria", i);
		}
		canhoto.removerAnexo(1L, "maria", false, "Duplicado");

		AnexoCanhoto denovo = canhoto.anexar("foto1.jpg", TipoAnexo.JPEG, 1024, sha(1));

		assertThat(canhoto.getAnexosAtivos()).hasSize(Canhoto.MAXIMO_ANEXOS).contains(denovo);
	}

	@Test
	void adminRemoveAnexoDeOutroUsuarioMesmoComCargaEmOcorrencia() {
		Canhoto canhoto = novoCanhoto();
		AnexoCanhoto anexo = anexarComo(canhoto, 1L, "maria", 1);
		canhoto.getNotaFiscal().getCarga().alterarStatus(StatusCarga.OCORRENCIA, "Cliente contestou a entrega");

		canhoto.removerAnexo(1L, "admin", true, "Enviado na NF errada");

		assertThat(anexo.isAtivo()).isFalse();
		assertThat(anexo.getRemovidoPor()).isEqualTo("admin");
	}

	@Test
	void outroOperadorNaoRemove() {
		Canhoto canhoto = novoCanhoto();
		AnexoCanhoto anexo = anexarComo(canhoto, 1L, "maria", 1);

		assertThatThrownBy(() -> canhoto.removerAnexo(1L, "joao", false, "Não gostei"))
				.isInstanceOf(OperacaoNaoPermitidaException.class)
				.hasMessage("Só quem enviou o anexo ou um ADMIN pode removê-lo");
		assertThat(anexo.isAtivo()).isTrue();
	}

	@Test
	void removerExigeMotivo() {
		Canhoto canhoto = novoCanhoto();
		anexarComo(canhoto, 1L, "maria", 1);

		assertThatThrownBy(() -> canhoto.removerAnexo(1L, "maria", false, "   "))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("Motivo é obrigatório para remover o anexo");
		assertThatThrownBy(() -> canhoto.removerAnexo(1L, "maria", false, null))
				.isInstanceOf(RegraNegocioException.class);
	}

	@Test
	void removerAnexoInexistenteOuJaRemovidoLancaExcecao() {
		Canhoto canhoto = novoCanhoto();
		anexarComo(canhoto, 1L, "maria", 1);
		canhoto.removerAnexo(1L, "maria", false, "Errado");

		assertThatThrownBy(() -> canhoto.removerAnexo(1L, "maria", false, "De novo"))
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessage("Anexo não encontrado: 1");
		assertThatThrownBy(() -> canhoto.removerAnexo(9L, "admin", true, "Qualquer"))
				.isInstanceOf(RecursoNaoEncontradoException.class);
	}
}
