package br.com.controlecanhotos.canhoto;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.com.controlecanhotos.canhoto.dto.AnexoResponse;
import br.com.controlecanhotos.canhoto.dto.ArquivoAnexo;
import br.com.controlecanhotos.canhoto.dto.RemoverAnexoRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/notas-fiscais/{notaFiscalId}/canhoto/anexos")
public class AnexoCanhotoController {

	private final AnexoCanhotoService service;

	public AnexoCanhotoController(AnexoCanhotoService service) {
		this.service = service;
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	ResponseEntity<AnexoResponse> anexar(@PathVariable Long notaFiscalId,
			@RequestParam("arquivo") MultipartFile arquivo) {
		AnexoResponse anexo = service.anexar(notaFiscalId, arquivo);
		return ResponseEntity
				.created(URI.create("/notas-fiscais/%d/canhoto/anexos/%d/arquivo".formatted(notaFiscalId, anexo.id())))
				.body(anexo);
	}

	/** Remoção lógica: só quem enviou o anexo ou um ADMIN (ADR 0006). */
	@PostMapping("/{anexoId}/remocao")
	ResponseEntity<Void> remover(@PathVariable Long notaFiscalId, @PathVariable Long anexoId,
			@RequestBody @Valid RemoverAnexoRequest request, Authentication autenticacao) {
		boolean admin = autenticacao.getAuthorities().stream()
				.anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
		service.remover(notaFiscalId, anexoId, request.motivo(), autenticacao.getName(), admin);
		return ResponseEntity.noContent().build();
	}

	/**
	 * Sempre como download ({@code attachment}); o Spring Security já manda {@code X-Content-Type-Options:
	 * nosniff}, para o navegador não tratar o arquivo como página (ADR 0006).
	 */
	@GetMapping("/{anexoId}/arquivo")
	ResponseEntity<byte[]> baixar(@PathVariable Long notaFiscalId, @PathVariable Long anexoId) {
		ArquivoAnexo arquivo = service.baixar(notaFiscalId, anexoId);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(arquivo.tipo().getTipoConteudo()))
				.contentLength(arquivo.conteudo().length)
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
						.filename(arquivo.nomeArquivo(), StandardCharsets.UTF_8).build().toString())
				.body(arquivo.conteudo());
	}
}
