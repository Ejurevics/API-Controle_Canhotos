package br.com.controlecanhotos.canhoto;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.controlecanhotos.canhoto.dto.CanhotoResponse;
import br.com.controlecanhotos.canhoto.dto.RegistrarCanhotoRequest;
import br.com.controlecanhotos.carga.Carga;
import br.com.controlecanhotos.carga.CargaRepository;
import br.com.controlecanhotos.shared.exception.RecursoNaoEncontradoException;
import br.com.controlecanhotos.shared.exception.RegraNegocioException;

@Service
public class CanhotoService {

	private final CargaRepository cargaRepository;

	public CanhotoService(CargaRepository cargaRepository) {
		this.cargaRepository = cargaRepository;
	}

	@Transactional(readOnly = true)
	public CanhotoResponse buscar(Long cargaId) {
		return CanhotoResponse.de(obter(cargaId).getCanhoto());
	}

	@Transactional
	public CanhotoResponse registrar(Long cargaId, RegistrarCanhotoRequest request) {
		Canhoto canhoto = obter(cargaId).getCanhoto();
		boolean confirmada = Boolean.TRUE.equals(request.dataFuturaConfirmada());

		switch (request.status()) {
			case RECEBIDO -> canhoto.registrarRecebimento(request.dataRecebimento(), confirmada);
			case DIVERGENTE -> canhoto.registrarDivergencia(request.dataRecebimento(), request.observacao(),
					confirmada);
			default -> throw new RegraNegocioException(
					"Só é possível registrar o canhoto como RECEBIDO ou DIVERGENTE");
		}
		return CanhotoResponse.de(canhoto);
	}

	private Carga obter(Long cargaId) {
		return cargaRepository.findById(cargaId)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Carga não encontrada: " + cargaId));
	}
}
