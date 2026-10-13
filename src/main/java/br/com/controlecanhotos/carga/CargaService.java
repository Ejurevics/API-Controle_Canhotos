package br.com.controlecanhotos.carga;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.controlecanhotos.carga.dto.AlterarStatusRequest;
import br.com.controlecanhotos.carga.dto.CargaDetalheResponse;
import br.com.controlecanhotos.shared.exception.RecursoNaoEncontradoException;

@Service
public class CargaService {

	private final CargaRepository cargaRepository;

	public CargaService(CargaRepository cargaRepository) {
		this.cargaRepository = cargaRepository;
	}

	@Transactional(readOnly = true)
	public CargaDetalheResponse buscar(Long id) {
		return CargaDetalheResponse.de(obter(id));
	}

	@Transactional
	public CargaDetalheResponse alterarStatus(Long id, AlterarStatusRequest request) {
		Carga carga = obter(id);
		carga.alterarStatus(request.status(), request.motivo());
		return CargaDetalheResponse.de(carga);
	}

	private Carga obter(Long id) {
		return cargaRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Carga não encontrada: " + id));
	}
}
