package br.com.controlecanhotos.canhoto;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.com.controlecanhotos.canhoto.dto.AnexoResponse;
import br.com.controlecanhotos.canhoto.dto.ArquivoAnexo;
import br.com.controlecanhotos.notafiscal.NotaFiscalRepository;
import br.com.controlecanhotos.shared.exception.ArquivoMuitoGrandeException;
import br.com.controlecanhotos.shared.exception.RecursoNaoEncontradoException;
import br.com.controlecanhotos.shared.exception.RegraNegocioException;

/** Envio e download do comprovante do canhoto (ADR 0006). */
@Service
public class AnexoCanhotoService {

	static final int TAMANHO_MAXIMO_NOME = 255;

	private static final String NOME_PADRAO = "comprovante";

	private final NotaFiscalRepository notaFiscalRepository;

	private final CanhotoRepository canhotoRepository;

	private final AnexoCanhotoConteudoRepository conteudoRepository;

	private final PropriedadesAnexo propriedades;

	public AnexoCanhotoService(NotaFiscalRepository notaFiscalRepository, CanhotoRepository canhotoRepository,
			AnexoCanhotoConteudoRepository conteudoRepository, PropriedadesAnexo propriedades) {
		this.notaFiscalRepository = notaFiscalRepository;
		this.canhotoRepository = canhotoRepository;
		this.conteudoRepository = conteudoRepository;
		this.propriedades = propriedades;
	}

	@Transactional
	public AnexoResponse anexar(Long notaFiscalId, MultipartFile arquivo) {
		Canhoto canhoto = obterCanhoto(notaFiscalId);
		long limite = propriedades.tamanhoMaximo().toBytes();
		if (arquivo.getSize() > limite) {
			throw new ArquivoMuitoGrandeException("Arquivo de %d bytes passa do limite de %d bytes"
					.formatted(arquivo.getSize(), limite));
		}
		byte[] conteudo = ler(arquivo);
		if (conteudo.length == 0) {
			throw new RegraNegocioException("Arquivo vazio");
		}
		TipoAnexo tipo = TipoAnexo.detectar(conteudo).orElseThrow(
				() -> new RegraNegocioException("Tipo de arquivo não aceito: envie JPEG, PNG ou PDF"));

		AnexoCanhoto anexo = canhoto.anexar(nomeArquivo(arquivo.getOriginalFilename(), tipo), tipo,
				conteudo.length, sha256(conteudo));
		// Grava os dados antes do conteúdo: a chave do conteúdo é o id do anexo, gerado pelo banco.
		canhotoRepository.flush();
		conteudoRepository.save(new AnexoCanhotoConteudo(anexo.getId(), conteudo));
		return AnexoResponse.de(anexo);
	}

	@Transactional(readOnly = true)
	public ArquivoAnexo baixar(Long notaFiscalId, Long anexoId) {
		AnexoCanhoto anexo = obterCanhoto(notaFiscalId).buscarAnexoAtivo(anexoId)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Anexo não encontrado: " + anexoId));
		byte[] conteudo = conteudoRepository.findById(anexoId)
				.orElseThrow(() -> new IllegalStateException("Anexo %d sem conteúdo".formatted(anexoId)))
				.getConteudo();
		return new ArquivoAnexo(anexo.getNomeArquivo(), anexo.getTipo(), conteudo);
	}

	/**
	 * Nome guardado: só o que vem depois da última barra (alguns navegadores mandam o caminho), sem caracteres
	 * de controle e com até {@value #TAMANHO_MAXIMO_NOME} caracteres. Sem nome, usa "comprovante" + extensão.
	 */
	static String nomeArquivo(String original, TipoAnexo tipo) {
		String nome = original == null ? "" : original;
		nome = nome.substring(Math.max(nome.lastIndexOf('/'), nome.lastIndexOf('\\')) + 1);
		nome = nome.replaceAll("\\p{Cntrl}", "").strip();
		if (nome.isEmpty()) {
			return NOME_PADRAO + tipo.getExtensao();
		}
		return nome.codePoints().limit(TAMANHO_MAXIMO_NOME)
				.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
	}

	static String sha256(byte[] conteudo) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(conteudo));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 indisponível na JVM", e);
		}
	}

	private static byte[] ler(MultipartFile arquivo) {
		try {
			return arquivo.getBytes();
		} catch (IOException e) {
			throw new UncheckedIOException("Falha ao ler o arquivo enviado", e);
		}
	}

	private Canhoto obterCanhoto(Long notaFiscalId) {
		return notaFiscalRepository.findById(notaFiscalId)
				.orElseThrow(() -> new RecursoNaoEncontradoException(
						"Nota fiscal não encontrada: " + notaFiscalId))
				.getCanhoto();
	}
}
