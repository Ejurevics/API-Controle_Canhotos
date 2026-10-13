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
@RequestMapping("/cargas/{cargaId}/canhoto")
public class CanhotoController {

	private final CanhotoService service;

	public CanhotoController(CanhotoService service) {
		this.service = service;
	}

	@GetMapping
	CanhotoResponse buscar(@PathVariable Long cargaId) {
		return service.buscar(cargaId);
	}

	@PostMapping("/registros")
	CanhotoResponse registrar(@PathVariable Long cargaId, @RequestBody @Valid RegistrarCanhotoRequest request) {
		return service.registrar(cargaId, request);
	}
}
