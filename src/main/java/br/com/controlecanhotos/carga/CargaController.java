package br.com.controlecanhotos.carga;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.controlecanhotos.carga.dto.AlterarStatusRequest;
import br.com.controlecanhotos.carga.dto.CargaDetalheResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/cargas")
public class CargaController {

	private final CargaService service;

	public CargaController(CargaService service) {
		this.service = service;
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
