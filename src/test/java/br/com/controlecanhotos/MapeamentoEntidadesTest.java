package br.com.controlecanhotos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import br.com.controlecanhotos.canhoto.Canhoto;
import br.com.controlecanhotos.canhoto.StatusCanhoto;
import br.com.controlecanhotos.carga.Carga;
import br.com.controlecanhotos.carga.StatusCarga;
import br.com.controlecanhotos.coleta.Coleta;
import br.com.controlecanhotos.coleta.SituacaoColeta;
import br.com.controlecanhotos.historico.HistoricoCanhoto;
import br.com.controlecanhotos.historico.HistoricoStatus;
import br.com.controlecanhotos.notafiscal.NotaFiscal;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class MapeamentoEntidadesTest {

	private static final String CNPJ = "11111111000111";

	@Autowired
	private TestEntityManager em;

	private static Coleta novaColeta() {
		return new Coleta("Cliente A", CNPJ, LocalDate.of(2026, 10, 2));
	}

	private static void entregar(Carga carga) {
		carga.alterarStatus(StatusCarga.COLETADA, null);
		carga.alterarStatus(StatusCarga.EM_TRANSITO, null);
		carga.alterarStatus(StatusCarga.ENTREGUE, null);
	}

	@Test
	void persisteColetaComCteVariasNotasCanhotosEHistorico() {
		Coleta coleta = novaColeta();
		Carga carga = coleta.adicionarCarga(Cte.chave(1), "Campinas", "SP");
		carga.adicionarNotaFiscal(CNPJ, (short) 1, 1234, new BigDecimal("150.500"));
		carga.adicionarNotaFiscal(CNPJ, (short) 1, 1235, new BigDecimal("49.500"));
		carga.alterarStatus(StatusCarga.COLETADA, "Coletada pelo motorista");

		Long id = em.persistAndFlush(coleta).getId();
		em.clear();

		Coleta salva = em.find(Coleta.class, id);
		assertThat(salva.getSituacao()).isEqualTo(SituacaoColeta.PROGRAMADA);
		assertThat(salva.getCriadoEm()).isNotNull();

		Carga cargaSalva = salva.getCargas().getFirst();
		assertThat(cargaSalva.getChaveAcesso()).isEqualTo(Cte.chave(1));
		assertThat(cargaSalva.isChaveProvisoria()).isFalse();
		assertThat(cargaSalva.getStatus()).isEqualTo(StatusCarga.COLETADA);
		assertThat(cargaSalva.getPesoTotalKg()).isEqualByComparingTo("200");
		assertThat(cargaSalva.getNotasFiscais())
				.extracting(NotaFiscal::getNumeroNotaFiscal, nota -> nota.getCanhoto().getStatus())
				.containsExactly(tuple(1234, StatusCanhoto.PENDENTE), tuple(1235, StatusCanhoto.PENDENTE));
		assertThat(cargaSalva.getHistorico())
				.extracting(HistoricoStatus::getStatusAnterior, HistoricoStatus::getStatusNovo)
				.containsExactly(
						tuple(null, StatusCarga.AGUARDANDO_COLETA),
						tuple(StatusCarga.AGUARDANDO_COLETA, StatusCarga.COLETADA));
	}

	@Test
	void persisteCanhotoDivergenteDeUmaNota() {
		Coleta coleta = novaColeta();
		Carga carga = Cte.cargaComUmaNota(coleta, 1);
		entregar(carga);
		carga.getNotasFiscais().getFirst().getCanhoto()
				.registrarDivergencia(LocalDate.of(2026, 10, 5), "Assinatura ilegível", false);

		Long id = em.persistAndFlush(coleta).getId();
		em.clear();

		Canhoto salvo = em.find(Coleta.class, id).getCargas().getFirst().getNotasFiscais().getFirst()
				.getCanhoto();
		assertThat(salvo.getStatus()).isEqualTo(StatusCanhoto.DIVERGENTE);
		assertThat(salvo.getDataRecebimento()).isEqualTo(LocalDate.of(2026, 10, 5));
		assertThat(salvo.getObservacao()).isEqualTo("Assinatura ilegível");
		assertThat(salvo.getHistorico())
				.extracting(HistoricoCanhoto::getStatusAnterior, HistoricoCanhoto::getStatusNovo,
						HistoricoCanhoto::getObservacao)
				.containsExactly(
						tuple(null, StatusCanhoto.PENDENTE, null),
						tuple(StatusCanhoto.PENDENTE, StatusCanhoto.DIVERGENTE, "Assinatura ilegível"));
		assertThat(salvo.getHistorico().getLast().getRegistradoEm()).isNotNull();
	}

	@Test
	void persisteConfirmacaoDeDataFuturaNoHistoricoDoCanhoto() {
		Coleta coleta = novaColeta();
		Carga carga = Cte.cargaComUmaNota(coleta, 1);
		entregar(carga);
		carga.getNotasFiscais().getFirst().getCanhoto().registrarRecebimento(LocalDate.now().plusYears(1), true);

		Long id = em.persistAndFlush(coleta).getId();
		em.clear();

		Canhoto salvo = em.find(Coleta.class, id).getCargas().getFirst().getNotasFiscais().getFirst()
				.getCanhoto();
		assertThat(salvo.getHistorico())
				.extracting(HistoricoCanhoto::isDataFuturaConfirmada)
				.containsExactly(false, true);
	}

	@Test
	void contagensDaCargaVemDoBancoEAcompanhamOsCanhotos() {
		Coleta coleta = novaColeta();
		Carga carga = coleta.adicionarCarga(Cte.chave(1), "Campinas", "SP");
		carga.adicionarNotaFiscal(CNPJ, (short) 1, 1, BigDecimal.TEN);
		carga.adicionarNotaFiscal(CNPJ, (short) 1, 2, BigDecimal.TEN);
		carga.adicionarNotaFiscal(CNPJ, (short) 1, 3, BigDecimal.TEN);
		entregar(carga);
		carga.getNotasFiscais().getFirst().getCanhoto().registrarRecebimento(LocalDate.of(2026, 10, 5), false);

		em.persistAndFlush(coleta);
		Long cargaId = carga.getId();
		em.clear();

		Carga lida = em.find(Carga.class, cargaId);
		assertThat(lida.getQuantidadeNotasFiscais()).isEqualTo(3);
		assertThat(lida.getCanhotosPendentes()).isEqualTo(2);
	}

	@Test
	void bloqueiaMesmaChaveDeAcessoEmColetasDiferentes() {
		em.persistAndFlush(coletaComCte(Cte.chave(1), 1234));

		Coleta segunda = coletaComCte(Cte.chave(1), 9999);

		assertThatThrownBy(() -> em.persistAndFlush(segunda))
				.isInstanceOf(ConstraintViolationException.class)
				.hasMessageContaining("uk_carga_chave_acesso");
	}

	@Test
	void permiteMesmaNotaEmCtesDiferentes() {
		em.persistAndFlush(coletaComCte(Cte.chave(1), 1234));
		Coleta segunda = coletaComCte(Cte.chave(2), 1234);

		Long id = em.persistAndFlush(segunda).getId();
		em.clear();

		Carga salva = em.find(Coleta.class, id).getCargas().getFirst();
		assertThat(salva.getNotasFiscais()).singleElement()
				.satisfies(nota -> assertThat(nota.getNumeroNotaFiscal()).isEqualTo(1234));
	}

	private static Coleta coletaComCte(String chave, int numeroNotaFiscal) {
		Coleta coleta = novaColeta();
		coleta.adicionarCarga(chave, "Campinas", "SP")
				.adicionarNotaFiscal(CNPJ, (short) 1, numeroNotaFiscal, BigDecimal.TEN);
		return coleta;
	}
}
