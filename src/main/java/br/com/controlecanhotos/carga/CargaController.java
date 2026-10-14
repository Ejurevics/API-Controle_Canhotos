package br.com.controlecanhotos.carga;

import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.controlecanhotos.carga.dto.AlterarStatusRequest;
import br.com.controlecanhotos.carga.dto.CargaDetalheResponse;
import br.com.controlecanhotos.carga.dto.CargaResponse;
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
	PagedModel<CargaResponse> listar(
			@RequestParam(required = false) StatusCarga status,
			@RequestParam(required = false) Long coletaId,
			@RequestParam(defaultValue = "0") int pagina,
			@RequestParam(defaultValue = "" + ParametrosListagem.TAMANHO_PADRAO) int tamanho) {
		return service.listar(status, coletaId, pagina, tamanho);
	}

	@GetMapping("/{id}")
	CargaDetalheResponse buscar(@PathVariable Long id) {
		return service.buscar(id);
	}

	@PatchMapping("/{id}/status")
	CargaDetalheResponse alterarStatus(@PathVariable Long id, @RequestBody @Valid AlterarStatusRequest request) {
		return service.alterarStatus(id, request);
	}
}
