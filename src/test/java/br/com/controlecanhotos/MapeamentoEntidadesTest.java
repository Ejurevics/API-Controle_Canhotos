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

import br.com.controlecanhotos.canhoto.StatusCanhoto;
import br.com.controlecanhotos.carga.Carga;
import br.com.controlecanhotos.carga.StatusCarga;
import br.com.controlecanhotos.coleta.Coleta;
import br.com.controlecanhotos.coleta.SituacaoColeta;
import br.com.controlecanhotos.historico.HistoricoStatus;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class MapeamentoEntidadesTest {

	@Autowired
	private TestEntityManager em;

	@Test
	void persisteColetaComCargaCanhotoEHistorico() {
		Coleta coleta = new Coleta("Cliente A", "11111111000111", LocalDate.of(2026, 10, 2));
		Carga carga = coleta.adicionarCarga("11111111000111", (short) 1, 1234,
				new BigDecimal("150.500"), "Campinas", "SP");
		carga.alterarStatus(StatusCarga.COLETADA, "Coletada pelo motorista");

		Long id = em.persistAndFlush(coleta).getId();
		em.clear();

		Coleta salva = em.find(Coleta.class, id);
		assertThat(salva.getSituacao()).isEqualTo(SituacaoColeta.PROGRAMADA);
		assertThat(salva.getCriadoEm()).isNotNull();

		Carga cargaSalva = salva.getCargas().getFirst();
		assertThat(cargaSalva.getNumeroNotaFiscal()).isEqualTo(1234);
		assertThat(cargaSalva.getPesoKg()).isEqualByComparingTo("150.5");
		assertThat(cargaSalva.getStatus()).isEqualTo(StatusCarga.COLETADA);
		assertThat(cargaSalva.getCanhoto().getStatus()).isEqualTo(StatusCanhoto.PENDENTE);
		assertThat(cargaSalva.getHistorico())
				.extracting(HistoricoStatus::getStatusAnterior, HistoricoStatus::getStatusNovo)
				.containsExactly(
						tuple(null, StatusCarga.AGUARDANDO_COLETA),
						tuple(StatusCarga.AGUARDANDO_COLETA, StatusCarga.COLETADA));
	}

	@Test
	void bloqueiaMesmaNotaFiscalEmColetasDiferentes() {
		Coleta primeira = new Coleta("Cliente A", "11111111000111", LocalDate.of(2026, 10, 2));
		primeira.adicionarCarga("11111111000111", (short) 1, 1234, BigDecimal.TEN, "Campinas", "SP");
		em.persistAndFlush(primeira);

		Coleta segunda = new Coleta("Cliente A", "11111111000111", LocalDate.of(2026, 10, 3));
		segunda.adicionarCarga("11111111000111", (short) 1, 1234, BigDecimal.ONE, "Santos", "SP");

		assertThatThrownBy(() -> em.persistAndFlush(segunda))
				.isInstanceOf(ConstraintViolationException.class)
				.hasMessageContaining("uk_carga_nota_fiscal");
	}
}
