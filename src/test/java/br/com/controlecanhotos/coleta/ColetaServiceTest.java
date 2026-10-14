package br.com.controlecanhotos.coleta;

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
import br.com.controlecanhotos.carga.CargaRepository;
import br.com.controlecanhotos.carga.StatusCarga;
import br.com.controlecanhotos.carga.dto.CriarCargaRequest;
import br.com.controlecanhotos.canhoto.StatusCanhoto;
import br.com.controlecanhotos.coleta.dto.ColetaResponse;
import br.com.controlecanhotos.coleta.dto.CriarColetaRequest;
import br.com.controlecanhotos.notafiscal.dto.CriarNotaFiscalRequest;
import br.com.controlecanhotos.shared.exception.RecursoNaoEncontradoException;
import br.com.controlecanhotos.shared.exception.RegraNegocioException;

@ExtendWith(MockitoExtension.class)
class ColetaServiceTest {

	private static final String CNPJ_CLIENTE = "11111111000111";
	private static final String CNPJ_OUTRO = "22222222000122";

	@Mock
	private ColetaRepository coletaRepository;

	@Mock
	private CargaRepository cargaRepository;

	@InjectMocks
	private ColetaService service;

	private static CriarNotaFiscalRequest nota(Integer numero, String emitenteCnpj, Short serie) {
		return new CriarNotaFiscalRequest(emitenteCnpj, serie, numero, new BigDecimal("150.5"));
	}

	private static CriarCargaRequest cte(int chave, CriarNotaFiscalRequest... notas) {
		return new CriarCargaRequest(Cte.chave(chave), "Campinas", "SP", List.of(notas));
	}

	private static CriarColetaRequest request(CriarCargaRequest... cargas) {
		return new CriarColetaRequest("Cliente A", CNPJ_CLIENTE, LocalDate.of(2026, 10, 20), List.of(cargas));
	}

	private static Coleta coletaExistente() {
		return new Coleta("Cliente A", CNPJ_CLIENTE, LocalDate.of(2026, 10, 20));
	}

	@Test
	void criaColetaAplicandoEmitenteESeriePadraoNasNotas() {
		when(coletaRepository.save(any(Coleta.class))).thenAnswer(i -> i.getArgument(0));

		ColetaResponse resposta = service.criar(request(cte(1, nota(1001, null, null))));

		assertThat(resposta.situacao()).isEqualTo(SituacaoColeta.PROGRAMADA);
		assertThat(resposta.cargas()).singleElement().satisfies(c -> {
			assertThat(c.chaveAcesso()).isEqualTo(Cte.chave(1));
			assertThat(c.status()).isEqualTo(StatusCarga.AGUARDANDO_COLETA);
			assertThat(c.notasFiscais()).singleElement().satisfies(n -> {
				assertThat(n.emitenteCnpj()).isEqualTo(CNPJ_CLIENTE);
				assertThat(n.serieNotaFiscal()).isEqualTo((short) 1);
				assertThat(n.statusCanhoto()).isEqualTo(StatusCanhoto.PENDENTE);
			});
		});
		verify(cargaRepository).existsByChaveAcesso(Cte.chave(1));
	}

	@Test
	void criaColetaRespeitandoEmitenteESerieInformados() {
		when(coletaRepository.save(any(Coleta.class))).thenAnswer(i -> i.getArgument(0));

		ColetaResponse resposta = service.criar(request(cte(1, nota(1002, CNPJ_OUTRO, (short) 3))));

		assertThat(resposta.cargas().getFirst().notasFiscais()).singleElement().satisfies(n -> {
			assertThat(n.emitenteCnpj()).isEqualTo(CNPJ_OUTRO);
			assertThat(n.serieNotaFiscal()).isEqualTo((short) 3);
		});
	}

	@Test
	void criaCteComVariasNotasSomandoOPeso() {
		when(coletaRepository.save(any(Coleta.class))).thenAnswer(i -> i.getArgument(0));

		ColetaResponse resposta = service.criar(request(cte(1, nota(1001, null, null), nota(1002, null, null))));

		assertThat(resposta.cargas()).singleElement().satisfies(c -> {
			assertThat(c.notasFiscais()).hasSize(2);
			assertThat(c.pesoTotalKg()).isEqualByComparingTo("301");
		});
	}

	@Test
	void mesmoNumeroDeNotaEmCtesDiferentesEhPermitido() {
		when(coletaRepository.save(any(Coleta.class))).thenAnswer(i -> i.getArgument(0));

		ColetaResponse resposta = service.criar(request(cte(1, nota(1001, null, null)), cte(2, nota(1001, null, null))));

		assertThat(resposta.cargas()).hasSize(2);
	}

	@Test
	void rejeitaNotaRepetidaNoMesmoCte() {
		assertThatThrownBy(() -> service.criar(request(cte(1, nota(1003, null, null), nota(1003, null, null)))))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining("repetida no CT-e");

		verify(coletaRepository, never()).save(any());
	}

	@Test
	void mesmoNumeroComSerieDiferenteNoMesmoCteNaoEhRepeticao() {
		when(coletaRepository.save(any(Coleta.class))).thenAnswer(i -> i.getArgument(0));

		ColetaResponse resposta = service.criar(
				request(cte(1, nota(1004, null, (short) 1), nota(1004, null, (short) 2))));

		assertThat(resposta.cargas().getFirst().notasFiscais()).hasSize(2);
	}

	@Test
	void rejeitaCteRepetidoNaRequisicao() {
		assertThatThrownBy(() -> service.criar(request(cte(1, nota(1001, null, null)), cte(1, nota(1002, null, null)))))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining("CT-e repetido na requisição");

		verify(coletaRepository, never()).save(any());
	}

	@Test
	void rejeitaCteJaCadastrado() {
		when(cargaRepository.existsByChaveAcesso(Cte.chave(1))).thenReturn(true);

		assertThatThrownBy(() -> service.criar(request(cte(1, nota(1005, null, null)))))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining("CT-e já cadastrado");

		verify(coletaRepository, never()).save(any());
	}

	@Test
	void buscaColetaExistente() {
		when(coletaRepository.findById(1L)).thenReturn(Optional.of(coletaExistente()));

		ColetaResponse resposta = service.buscar(1L);

		assertThat(resposta.clienteCnpj()).isEqualTo(CNPJ_CLIENTE);
	}

	@Test
	void buscaColetaInexistenteLancaNaoEncontrado() {
		when(coletaRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.buscar(99L))
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessageContaining("99");
	}

	@Test
	void alteraSituacaoSeguindoOFluxo() {
		when(coletaRepository.findById(1L)).thenReturn(Optional.of(coletaExistente()));

		ColetaResponse resposta = service.alterarSituacao(1L, SituacaoColeta.EM_ANDAMENTO);

		assertThat(resposta.situacao()).isEqualTo(SituacaoColeta.EM_ANDAMENTO);
	}

	@Test
	void rejeitaTransicaoDeSituacaoInvalida() {
		when(coletaRepository.findById(1L)).thenReturn(Optional.of(coletaExistente()));

		assertThatThrownBy(() -> service.alterarSituacao(1L, SituacaoColeta.CONCLUIDA))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining("PROGRAMADA -> CONCLUIDA");
	}

	@Test
	void alterarSituacaoDeColetaInexistenteLancaNaoEncontrado() {
		when(coletaRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.alterarSituacao(99L, SituacaoColeta.EM_ANDAMENTO))
				.isInstanceOf(RecursoNaoEncontradoException.class);
	}

	@Test
	void listaOrdenandoPelaDataProgramadaDecrescente() {
		when(coletaRepository.findAll(any(Specification.class), any(Pageable.class)))
				.thenAnswer(i -> new PageImpl<>(List.of(coletaExistente()), i.getArgument(1), 1));

		var pagina = service.listar(SituacaoColeta.PROGRAMADA, CNPJ_CLIENTE, LocalDate.of(2026, 10, 1),
				LocalDate.of(2026, 10, 31), 2, 10);

		ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
		verify(coletaRepository).findAll(any(Specification.class), captor.capture());
		Pageable pageable = captor.getValue();
		assertThat(pageable.getPageNumber()).isEqualTo(2);
		assertThat(pageable.getPageSize()).isEqualTo(10);
		assertThat(pageable.getSort().toString()).isEqualTo("dataProgramada: DESC,id: DESC");
		assertThat(pagina.getContent()).singleElement()
				.satisfies(c -> assertThat(c.clienteCnpj()).isEqualTo(CNPJ_CLIENTE));
	}

	@Test
	void listaSemFiltrosFunciona() {
		when(coletaRepository.findAll(any(Specification.class), any(Pageable.class)))
				.thenAnswer(i -> new PageImpl<>(List.of(), i.getArgument(1), 0));

		var pagina = service.listar(null, null, null, null, 0, 20);

		assertThat(pagina.getContent()).isEmpty();
	}

	@Test
	void rejeitaPeriodoInvertidoSemConsultarOBanco() {
		assertThatThrownBy(() -> service.listar(null, null, LocalDate.of(2026, 10, 10),
				LocalDate.of(2026, 10, 1), 0, 20))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining("dataInicio");

		verifyNoInteractions(coletaRepository);
	}

	@Test
	void rejeitaPaginacaoForaDoLimiteSemConsultarOBanco() {
		assertThatThrownBy(() -> service.listar(null, null, null, null, -1, 20))
				.isInstanceOf(RegraNegocioException.class);
		assertThatThrownBy(() -> service.listar(null, null, null, null, 0, 0))
				.isInstanceOf(RegraNegocioException.class);
		assertThatThrownBy(() -> service.listar(null, null, null, null, 0, 101))
				.isInstanceOf(RegraNegocioException.class);

		verifyNoInteractions(coletaRepository);
	}
}
