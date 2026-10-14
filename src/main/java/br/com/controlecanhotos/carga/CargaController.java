package br.com.controlecanhotos.carga;

import org.springframework.data.web.PagedModel;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.controlecanhotos.carga.dto.AlterarStatusRequest;
import br.com.controlecanhotos.carga.dto.CargaDetalheResponse;
import br.com.controlecanhotos.carga.dto.CargaResumoResponse;
import br.com.controlecanhotos.carga.dto.CorrigirChaveAcessoRequest;
import br.com.controlecanhotos.shared.paginacao.ParametrosListagem;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/cargas")
public class CargaController {

	private final CargaService service;

	public CargaController(CargaService service) {
		this.service = service;
	}

	@GetMapping
	PagedModel<CargaResumoResponse> listar(
			@RequestParam(required = false) StatusCarga status,
			@RequestParam(required = false) Long coletaId,
			@RequestParam(required = false) Boolean chaveProvisoria,
			@RequestParam(defaultValue = "0") int pagina,
			@RequestParam(defaultValue = "" + ParametrosListagem.TAMANHO_PADRAO) int tamanho) {
		return service.listar(status, coletaId, chaveProvisoria, pagina, tamanho);
	}

	@GetMapping("/{id}")
	CargaDetalheResponse buscar(@PathVariable Long id) {
		return service.buscar(id);
	}

	@PatchMapping("/{id}/status")
	CargaDetalheResponse alterarStatus(@PathVariable Long id, @RequestBody @Valid AlterarStatusRequest request) {
		return service.alterarStatus(id, request);
	}

	@PatchMapping("/{id}/chave-acesso")
	CargaDetalheResponse corrigirChaveAcesso(@PathVariable Long id,
			@RequestBody @Valid CorrigirChaveAcessoRequest request, @AuthenticationPrincipal Jwt jwt) {
		return service.corrigirChaveAcesso(id, request.chaveAcesso(), jwt.getSubject());
	}
}
