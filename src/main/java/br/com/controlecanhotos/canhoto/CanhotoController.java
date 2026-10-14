package br.com.controlecanhotos.canhoto;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.controlecanhotos.canhoto.dto.CanhotoResponse;
import br.com.controlecanhotos.canhoto.dto.RegistrarCanhotoRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/notas-fiscais/{notaFiscalId}/canhoto")
public class CanhotoController {

	private final CanhotoService service;

	public CanhotoController(CanhotoService service) {
		this.service = service;
	}

	@GetMapping
	CanhotoResponse buscar(@PathVariable Long notaFiscalId) {
		return service.buscar(notaFiscalId);
	}

	@PostMapping("/registros")
	CanhotoResponse registrar(@PathVariable Long notaFiscalId, @RequestBody @Valid RegistrarCanhotoRequest request) {
		return service.registrar(notaFiscalId, request);
	}
}
