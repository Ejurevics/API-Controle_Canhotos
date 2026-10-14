package br.com.controlecanhotos.canhoto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
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
import br.com.controlecanhotos.canhoto.dto.CanhotoResponse;
import br.com.controlecanhotos.canhoto.dto.RegistrarCanhotoRequest;
import br.com.controlecanhotos.carga.Carga;
import br.com.controlecanhotos.notafiscal.NotaFiscal;
import br.com.controlecanhotos.notafiscal.NotaFiscalRepository;
import br.com.controlecanhotos.carga.StatusCarga;
import br.com.controlecanhotos.coleta.Coleta;
import br.com.controlecanhotos.shared.exception.ConfirmacaoNecessariaException;
import br.com.controlecanhotos.shared.exception.RecursoNaoEncontradoException;
import br.com.controlecanhotos.shared.exception.RegraNegocioException;

@ExtendWith(MockitoExtension.class)
class CanhotoServiceTest {

	@Mock
	private NotaFiscalRepository notaFiscalRepository;

	@Mock
	private CanhotoRepository canhotoRepository;

	@InjectMocks
	private CanhotoService service;

	/** Mesmo fuso que o domínio usa para decidir se a data é futura. */
	private static LocalDate hoje() {
		return LocalDate.now(ZoneId.of("America/Sao_Paulo"));
	}

	private static NotaFiscal notaNoStatus(StatusCarga destino) {
		Carga carga = new Coleta("Cliente A", "11111111000111", LocalDate.of(2026, 10, 20))
				.adicionarCarga(Cte.chave(1), "Campinas", "SP");
		NotaFiscal nota = carga.adicionarNotaFiscal("11111111000111", (short) 1, 1001, new BigDecimal("10"));
		if (destino != StatusCarga.AGUARDANDO_COLETA) {
			carga.alterarStatus(StatusCarga.COLETADA, null);
			carga.alterarStatus(StatusCarga.EM_TRANSITO, null);
			carga.alterarStatus(StatusCarga.ENTREGUE, null);
		}
		return nota;
	}

	private static RegistrarCanhotoRequest registro(StatusCanhoto status, LocalDate data, String observacao,
			Boolean confirmada) {
		return new RegistrarCanhotoRequest(status, data, observacao, confirmada);
	}

	@Test
	void buscaCanhotoPendenteDaNota() {
		when(notaFiscalRepository.findById(1L)).thenReturn(Optional.of(notaNoStatus(StatusCarga.AGUARDANDO_COLETA)));

		CanhotoResponse resposta = service.buscar(1L);

		assertThat(resposta.status()).isEqualTo(StatusCanhoto.PENDENTE);
		assertThat(resposta.dataRecebimento()).isNull();
		assertThat(resposta.historico()).hasSize(1);
	}

	@Test
	void buscaCanhotoDeNotaInexistenteLancaNaoEncontrado() {
		when(notaFiscalRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.buscar(99L))
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessageContaining("99");
	}

	@Test
	void registraRecebimentoComCargaEntregue() {
		when(notaFiscalRepository.findById(1L)).thenReturn(Optional.of(notaNoStatus(StatusCarga.ENTREGUE)));

		CanhotoResponse resposta = service.registrar(1L, registro(StatusCanhoto.RECEBIDO, hoje(), null, null));

		assertThat(resposta.status()).isEqualTo(StatusCanhoto.RECEBIDO);
		assertThat(resposta.dataRecebimento()).isEqualTo(hoje());
		assertThat(resposta.historico()).hasSize(2);
		assertThat(resposta.historico().get(1).dataFuturaConfirmada()).isFalse();
	}

	@Test
	void registraDivergenciaComObservacao() {
		when(notaFiscalRepository.findById(1L)).thenReturn(Optional.of(notaNoStatus(StatusCarga.ENTREGUE)));

		CanhotoResponse resposta = service.registrar(1L,
				registro(StatusCanhoto.DIVERGENTE, hoje(), "Assinatura ilegível", false));

		assertThat(resposta.status()).isEqualTo(StatusCanhoto.DIVERGENTE);
		assertThat(resposta.observacao()).isEqualTo("Assinatura ilegível");
	}

	@Test
	void divergenciaSemObservacaoEhRejeitada() {
		when(notaFiscalRepository.findById(1L)).thenReturn(Optional.of(notaNoStatus(StatusCarga.ENTREGUE)));

		assertThatThrownBy(() -> service.registrar(1L, registro(StatusCanhoto.DIVERGENTE, hoje(), " ", false)))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining("Observação");
	}

	@Test
	void divergenciaDepoisDeRecebimentoPreservaOHistorico() {
		when(notaFiscalRepository.findById(1L)).thenReturn(Optional.of(notaNoStatus(StatusCarga.ENTREGUE)));

		service.registrar(1L, registro(StatusCanhoto.DIVERGENTE, hoje(), "Rasurado", false));
		CanhotoResponse resposta = service.registrar(1L, registro(StatusCanhoto.RECEBIDO, hoje(), null, false));

		assertThat(resposta.status()).isEqualTo(StatusCanhoto.RECEBIDO);
		assertThat(resposta.observacao()).isNull();
		assertThat(resposta.historico()).extracting(CanhotoResponse.Registro::statusNovo)
				.containsExactly(StatusCanhoto.PENDENTE, StatusCanhoto.DIVERGENTE, StatusCanhoto.RECEBIDO);
	}

	@Test
	void exigeCargaEntregue() {
		when(notaFiscalRepository.findById(1L)).thenReturn(Optional.of(notaNoStatus(StatusCarga.AGUARDANDO_COLETA)));

		assertThatThrownBy(() -> service.registrar(1L, registro(StatusCanhoto.RECEBIDO, hoje(), null, false)))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining("ENTREGUE");
	}

	@Test
	void naoPermiteRegistrarComoPendente() {
		when(notaFiscalRepository.findById(1L)).thenReturn(Optional.of(notaNoStatus(StatusCarga.ENTREGUE)));

		assertThatThrownBy(() -> service.registrar(1L, registro(StatusCanhoto.PENDENTE, hoje(), null, false)))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining("RECEBIDO ou DIVERGENTE");
	}

	@Test
	void dataFuturaPedeConfirmacaoESoPassaQuandoConfirmada() {
		NotaFiscal nota = notaNoStatus(StatusCarga.ENTREGUE);
		when(notaFiscalRepository.findById(1L)).thenReturn(Optional.of(nota));
		LocalDate futura = hoje().plusDays(30);

		assertThatThrownBy(() -> service.registrar(1L, registro(StatusCanhoto.RECEBIDO, futura, null, null)))
				.isInstanceOf(ConfirmacaoNecessariaException.class);
		assertThat(nota.getCanhoto().getStatus()).isEqualTo(StatusCanhoto.PENDENTE);

		CanhotoResponse resposta = service.registrar(1L, registro(StatusCanhoto.RECEBIDO, futura, null, true));

		assertThat(resposta.status()).isEqualTo(StatusCanhoto.RECEBIDO);
		assertThat(resposta.historico().get(1).dataFuturaConfirmada()).isTrue();
	}

	@Test
	void confirmacaoComDataNaoFuturaNaoMarcaOHistorico() {
		when(notaFiscalRepository.findById(1L)).thenReturn(Optional.of(notaNoStatus(StatusCarga.ENTREGUE)));

		CanhotoResponse resposta = service.registrar(1L, registro(StatusCanhoto.RECEBIDO, hoje(), null, true));

		assertThat(resposta.historico().get(1).dataFuturaConfirmada()).isFalse();
	}

	@Test
	void registrarEmNotaInexistenteLancaNaoEncontrado() {
		when(notaFiscalRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.registrar(99L, registro(StatusCanhoto.RECEBIDO, hoje(), null, false)))
				.isInstanceOf(RecursoNaoEncontradoException.class);
	}

	@Test
	void listaOrdenandoPorIdComAPaginaPedida() {
		NotaFiscal nota = notaNoStatus(StatusCarga.AGUARDANDO_COLETA);
		when(canhotoRepository.findAll(any(Specification.class), any(Pageable.class)))
				.thenAnswer(i -> new PageImpl<>(List.of(nota.getCanhoto()), i.getArgument(1), 1));

		var pagina = service.listar(StatusCanhoto.PENDENTE, "11111111000111", LocalDate.of(2026, 10, 1),
				LocalDate.of(2026, 10, 31), 0, 50);

		ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
		verify(canhotoRepository).findAll(any(Specification.class), captor.capture());
		assertThat(captor.getValue().getPageSize()).isEqualTo(50);
		assertThat(captor.getValue().getSort().toString()).isEqualTo("id: ASC");
		assertThat(pagina.getContent()).singleElement().satisfies(c -> {
			assertThat(c.status()).isEqualTo(StatusCanhoto.PENDENTE);
			assertThat(c.clienteCnpj()).isEqualTo("11111111000111");
			assertThat(c.numeroNotaFiscal()).isEqualTo(1001);
			assertThat(c.chaveAcesso()).isEqualTo(Cte.chave(1));
			assertThat(c.dataProgramada()).isEqualTo(LocalDate.of(2026, 10, 20));
		});
	}

	@Test
	void rejeitaPeriodoInvertidoSemConsultarOBanco() {
		assertThatThrownBy(() -> service.listar(null, null, LocalDate.of(2026, 10, 10),
				LocalDate.of(2026, 10, 1), 0, 20))
				.isInstanceOf(RegraNegocioException.class);

		verifyNoInteractions(canhotoRepository);
	}

	@Test
	void rejeitaPaginacaoForaDoLimiteSemConsultarOBanco() {
		assertThatThrownBy(() -> service.listar(null, null, null, null, -1, 20))
				.isInstanceOf(RegraNegocioException.class);

		verifyNoInteractions(canhotoRepository);
	}
}
