package br.com.controlecanhotos.carga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import br.com.controlecanhotos.Cte;
import br.com.controlecanhotos.canhoto.StatusCanhoto;
import br.com.controlecanhotos.carga.dto.AlterarStatusRequest;
import br.com.controlecanhotos.carga.dto.CargaDetalheResponse;
import br.com.controlecanhotos.coleta.Coleta;
import br.com.controlecanhotos.shared.exception.RecursoNaoEncontradoException;
import br.com.controlecanhotos.shared.exception.RegraNegocioException;

@ExtendWith(MockitoExtension.class)
class CargaServiceTest {

	@Mock
	private CargaRepository cargaRepository;

	@InjectMocks
	private CargaService service;

	private static Carga novaCarga() {
		Carga carga = new Coleta("Cliente A", "11111111000111", LocalDate.of(2026, 10, 20))
				.adicionarCarga(Cte.chave(1), "Campinas", "SP");
		carga.adicionarNotaFiscal("11111111000111", (short) 1, 1001, new BigDecimal("10"));
		return carga;
	}

	@Test
	void buscaCargaComHistoricoInicial() {
		when(cargaRepository.findById(1L)).thenReturn(Optional.of(novaCarga()));

		CargaDetalheResponse resposta = service.buscar(1L);

		assertThat(resposta.status()).isEqualTo(StatusCarga.AGUARDANDO_COLETA);
		assertThat(resposta.chaveAcesso()).isEqualTo(Cte.chave(1));
		assertThat(resposta.pesoTotalKg()).isEqualByComparingTo("10");
		assertThat(resposta.notasFiscais()).singleElement().satisfies(nota -> {
			assertThat(nota.numeroNotaFiscal()).isEqualTo(1001);
			assertThat(nota.statusCanhoto()).isEqualTo(StatusCanhoto.PENDENTE);
		});
		assertThat(resposta.historico()).singleElement().satisfies(h -> {
			assertThat(h.statusAnterior()).isNull();
			assertThat(h.statusNovo()).isEqualTo(StatusCarga.AGUARDANDO_COLETA);
		});
	}

	@Test
	void buscaCargaInexistenteLancaNaoEncontrado() {
		when(cargaRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.buscar(99L))
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessageContaining("99");
	}

	@Test
	void alteraStatusERegistraHistorico() {
		when(cargaRepository.findById(1L)).thenReturn(Optional.of(novaCarga()));

		CargaDetalheResponse resposta = service.alterarStatus(1L,
				new AlterarStatusRequest(StatusCarga.COLETADA, null));

		assertThat(resposta.status()).isEqualTo(StatusCarga.COLETADA);
		assertThat(resposta.historico()).hasSize(2);
		assertThat(resposta.historico().get(1).statusAnterior()).isEqualTo(StatusCarga.AGUARDANDO_COLETA);
		assertThat(resposta.historico().get(1).statusNovo()).isEqualTo(StatusCarga.COLETADA);
	}

	@Test
	void ocorrenciaGuardaOMotivoNoHistorico() {
		when(cargaRepository.findById(1L)).thenReturn(Optional.of(novaCarga()));

		CargaDetalheResponse resposta = service.alterarStatus(1L,
				new AlterarStatusRequest(StatusCarga.OCORRENCIA, "Cliente ausente"));

		assertThat(resposta.status()).isEqualTo(StatusCarga.OCORRENCIA);
		assertThat(resposta.historico().get(1).motivo()).isEqualTo("Cliente ausente");
	}

	@Test
	void ocorrenciaSemMotivoEhRejeitadaESemMudarOStatus() {
		Carga carga = novaCarga();
		when(cargaRepository.findById(1L)).thenReturn(Optional.of(carga));

		assertThatThrownBy(() -> service.alterarStatus(1L, new AlterarStatusRequest(StatusCarga.OCORRENCIA, " ")))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining("Motivo");

		assertThat(carga.getStatus()).isEqualTo(StatusCarga.AGUARDANDO_COLETA);
		assertThat(carga.getHistorico()).hasSize(1);
	}

	@Test
	void rejeitaTransicaoInvalida() {
		when(cargaRepository.findById(1L)).thenReturn(Optional.of(novaCarga()));

		assertThatThrownBy(() -> service.alterarStatus(1L, new AlterarStatusRequest(StatusCarga.ENTREGUE, null)))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining("AGUARDANDO_COLETA -> ENTREGUE");
	}

	@Test
	void alterarStatusDeCargaInexistenteLancaNaoEncontrado() {
		when(cargaRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.alterarStatus(99L, new AlterarStatusRequest(StatusCarga.COLETADA, null)))
				.isInstanceOf(RecursoNaoEncontradoException.class);
	}

	@Test
	void listaOrdenandoPorIdComAPaginaPedida() {
		when(cargaRepository.findAll(any(Specification.class), any(Pageable.class)))
				.thenAnswer(i -> new PageImpl<>(List.of(novaCarga()), i.getArgument(1), 1));

		var pagina = service.listar(StatusCarga.AGUARDANDO_COLETA, 7L, null, 1, 5);

		ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
		verify(cargaRepository).findAll(any(Specification.class), captor.capture());
		assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
		assertThat(captor.getValue().getPageSize()).isEqualTo(5);
		assertThat(captor.getValue().getSort().toString()).isEqualTo("id: ASC");
		assertThat(pagina.getContent()).singleElement()
				.satisfies(c -> {
					assertThat(c.chaveAcesso()).isEqualTo(Cte.chave(1));
					assertThat(c.status()).isEqualTo(StatusCarga.AGUARDANDO_COLETA);
				});
	}

	@Test
	void rejeitaPaginacaoForaDoLimiteSemConsultarOBanco() {
		assertThatThrownBy(() -> service.listar(null, null, null, 0, 101))
				.isInstanceOf(RegraNegocioException.class);

		verifyNoInteractions(cargaRepository);
	}

	@Test
	void corrigeChaveProvisoria() {
		when(cargaRepository.findById(1L)).thenReturn(Optional.of(CargaTest.cargaMigrada()));

		CargaDetalheResponse resposta = service.corrigirChaveAcesso(1L, Cte.chave(7), "admin");

		assertThat(resposta.chaveAcesso()).isEqualTo(Cte.chave(7));
		assertThat(resposta.chaveProvisoria()).isFalse();
		assertThat(resposta.correcaoChave().corrigidaPor()).isEqualTo("admin");
		assertThat(resposta.correcaoChave().chaveAnterior()).startsWith("99");
	}

	@Test
	void naoCorrigeParaChaveDeOutroCte() {
		Carga carga = CargaTest.cargaMigrada();
		String chaveOriginal = carga.getChaveAcesso();
		when(cargaRepository.findById(1L)).thenReturn(Optional.of(carga));
		when(cargaRepository.existsByChaveAcesso(Cte.chave(7))).thenReturn(true);

		assertThatThrownBy(() -> service.corrigirChaveAcesso(1L, Cte.chave(7), "admin"))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining("CT-e já cadastrado");
		assertThat(carga.getChaveAcesso()).isEqualTo(chaveOriginal);
		assertThat(carga.isChaveProvisoria()).isTrue();
	}

	@Test
	void naoCorrigeChaveDefinitivaNemConsultaDuplicidade() {
		when(cargaRepository.findById(1L)).thenReturn(Optional.of(novaCarga()));

		assertThatThrownBy(() -> service.corrigirChaveAcesso(1L, Cte.chave(7), "admin"))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining("já é definitiva");
		verify(cargaRepository, never()).existsByChaveAcesso(any());
	}

	@Test
	void corrigirChaveDeCargaInexistenteLancaNaoEncontrado() {
		when(cargaRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.corrigirChaveAcesso(99L, Cte.chave(7), "admin"))
				.isInstanceOf(RecursoNaoEncontradoException.class);
	}
}
