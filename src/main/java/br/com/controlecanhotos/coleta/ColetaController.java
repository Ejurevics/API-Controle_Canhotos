package br.com.controlecanhotos.coleta;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.controlecanhotos.coleta.dto.ColetaResponse;
import br.com.controlecanhotos.coleta.dto.CriarColetaRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/coletas")
public class ColetaController {

	private final ColetaService service;

	public ColetaController(ColetaService service) {
		this.service = service;
	}

	@PostMapping
	ResponseEntity<ColetaResponse> criar(@RequestBody @Valid CriarColetaRequest request) {
		ColetaResponse coleta = service.criar(request);
		return ResponseEntity.created(URI.create("/coletas/" + coleta.id())).body(coleta);
	}

	@GetMapping("/{id}")
	ColetaResponse buscar(@PathVariable Long id) {
		return service.buscar(id);
	}
}
