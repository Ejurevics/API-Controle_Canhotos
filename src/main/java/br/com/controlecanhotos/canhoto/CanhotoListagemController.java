package br.com.controlecanhotos.canhoto;

import java.time.LocalDate;

import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.controlecanhotos.canhoto.dto.CanhotoResumoResponse;
import br.com.controlecanhotos.shared.paginacao.ParametrosListagem;

@RestController
@RequestMapping("/canhotos")
public class CanhotoListagemController {

	private final CanhotoService service;

	public CanhotoListagemController(CanhotoService service) {
		this.service = service;
	}

	@GetMapping
	PagedModel<CanhotoResumoResponse> listar(
			@RequestParam(required = false) StatusCanhoto status,
			@RequestParam(required = false) String clienteCnpj,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
			@RequestParam(defaultValue = "0") int pagina,
			@RequestParam(defaultValue = "" + ParametrosListagem.TAMANHO_PADRAO) int tamanho) {
		return service.listar(status, clienteCnpj, dataInicio, dataFim, pagina, tamanho);
	}
}
