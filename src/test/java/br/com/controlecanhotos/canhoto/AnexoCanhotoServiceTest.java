package br.com.controlecanhotos.canhoto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import br.com.controlecanhotos.Cte;
import br.com.controlecanhotos.canhoto.dto.AnexoResponse;
import br.com.controlecanhotos.carga.Carga;
import br.com.controlecanhotos.carga.StatusCarga;
import br.com.controlecanhotos.coleta.Coleta;
import br.com.controlecanhotos.notafiscal.NotaFiscal;
import br.com.controlecanhotos.notafiscal.NotaFiscalRepository;
import br.com.controlecanhotos.shared.exception.ArquivoMuitoGrandeException;
import br.com.controlecanhotos.shared.exception.RecursoNaoEncontradoException;
import br.com.controlecanhotos.shared.exception.RegraNegocioException;

@ExtendWith(MockitoExtension.class)
class AnexoCanhotoServiceTest {

	@Mock
	private NotaFiscalRepository notaFiscalRepository;

	@Mock
	private CanhotoRepository canhotoRepository;

	@Mock
	private AnexoCanhotoConteudoRepository conteudoRepository;

	private AnexoCanhotoService service;

	@BeforeEach
	void criarService() {
		service = new AnexoCanhotoService(notaFiscalRepository, canhotoRepository, conteudoRepository,
				new PropriedadesAnexo(DataSize.ofBytes(100)));
	}

	private NotaFiscal notaEntregue() {
		Carga carga = new Coleta("Cliente A", "11111111000111", LocalDate.of(2026, 10, 20))
				.adicionarCarga(Cte.chave(1), "Campinas", "SP");
		NotaFiscal nota = carga.adicionarNotaFiscal("11111111000111", (short) 1, 1001, BigDecimal.TEN);
		carga.alterarStatus(StatusCarga.COLETADA, null);
		carga.alterarStatus(StatusCarga.EM_TRANSITO, null);
		carga.alterarStatus(StatusCarga.ENTREGUE, null);
		when(notaFiscalRepository.findById(1L)).thenReturn(Optional.of(nota));
		return nota;
	}

	private static MockMultipartFile arquivo(String nome, byte[] conteudo) {
		return new MockMultipartFile("arquivo", nome, "application/octet-stream", conteudo);
	}

	@Test
	void anexaPeloTipoDoConteudoEGravaOsBytes() {
		NotaFiscal nota = notaEntregue();

		// Declarado como JPEG, mas o conteúdo é PNG: vale o conteúdo.
		AnexoResponse resposta = service.anexar(1L,
				new MockMultipartFile("arquivo", "foto.jpg", "image/jpeg", TipoAnexoTest.PNG));

		assertThat(resposta.nomeArquivo()).isEqualTo("foto.jpg");
		assertThat(resposta.tipoConteudo()).isEqualTo("image/png");
		assertThat(resposta.tamanhoBytes()).isEqualTo(TipoAnexoTest.PNG.length);
		assertThat(nota.getCanhoto().getAnexosAtivos().getFirst().getSha256())
				.isEqualTo(AnexoCanhotoService.sha256(TipoAnexoTest.PNG)).hasSize(64);
		verify(canhotoRepository).flush();
		ArgumentCaptor<AnexoCanhotoConteudo> conteudo = ArgumentCaptor.forClass(AnexoCanhotoConteudo.class);
		verify(conteudoRepository).save(conteudo.capture());
		assertThat(conteudo.getValue().getConteudo()).isEqualTo(TipoAnexoTest.PNG);
		assertThat(conteudo.getValue().isNew()).isTrue();
	}

	@Test
	void arquivoAcimaDoLimiteLancaExcecao() {
		notaEntregue();

		assertThatThrownBy(() -> service.anexar(1L, arquivo("grande.pdf", new byte[101])))
				.isInstanceOf(ArquivoMuitoGrandeException.class)
				.hasMessage("Arquivo de 101 bytes passa do limite de 100 bytes");
		verify(conteudoRepository, never()).save(any());
	}

	@Test
	void arquivoVazioLancaExcecao() {
		notaEntregue();

		assertThatThrownBy(() -> service.anexar(1L, arquivo("vazio.pdf", new byte[0])))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("Arquivo vazio");
	}

	@Test
	void tipoNaoAceitoLancaExcecao() {
		notaEntregue();

		assertThatThrownBy(() -> service.anexar(1L, arquivo("pagina.html", "<html>".getBytes())))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("Tipo de arquivo não aceito: envie JPEG, PNG ou PDF");
	}

	@Test
	void falhaAoLerOArquivoViraExcecaoNaoVerificada() throws IOException {
		notaEntregue();
		MultipartFile arquivo = mock(MultipartFile.class);
		when(arquivo.getSize()).thenReturn(10L);
		when(arquivo.getBytes()).thenThrow(new IOException("conexão caiu"));

		assertThatThrownBy(() -> service.anexar(1L, arquivo))
				.isInstanceOf(UncheckedIOException.class)
				.hasMessage("Falha ao ler o arquivo enviado");
	}

	@Test
	void notaFiscalInexistenteLancaExcecao() {
		when(notaFiscalRepository.findById(9L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.anexar(9L, arquivo("a.pdf", TipoAnexoTest.PDF)))
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessage("Nota fiscal não encontrada: 9");
	}

	@Test
	void baixarAnexoInexistenteLancaExcecao() {
		notaEntregue();

		assertThatThrownBy(() -> service.baixar(1L, 5L))
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessage("Anexo não encontrado: 5");
	}

	@Test
	void nomeDoArquivoSemCaminhoNemControleELimitado() {
		assertThat(AnexoCanhotoService.nomeArquivo("C:\\fotos\\canhoto 1.jpg", TipoAnexo.JPEG))
				.isEqualTo("canhoto 1.jpg");
		assertThat(AnexoCanhotoService.nomeArquivo("../../etc/passwd.pdf", TipoAnexo.PDF)).isEqualTo("passwd.pdf");
		assertThat(AnexoCanhotoService.nomeArquivo("  nota\r\n.pdf ", TipoAnexo.PDF)).isEqualTo("nota.pdf");
		assertThat(AnexoCanhotoService.nomeArquivo(null, TipoAnexo.PNG)).isEqualTo("comprovante.png");
		assertThat(AnexoCanhotoService.nomeArquivo("pasta/", TipoAnexo.PDF)).isEqualTo("comprovante.pdf");
		assertThat(AnexoCanhotoService.nomeArquivo("😀".repeat(300), TipoAnexo.JPEG))
				.as("conta caracteres, sem partir um par substituto")
				.hasSize(AnexoCanhotoService.TAMANHO_MAXIMO_NOME * 2)
				.isEqualTo("😀".repeat(AnexoCanhotoService.TAMANHO_MAXIMO_NOME));
	}
}
