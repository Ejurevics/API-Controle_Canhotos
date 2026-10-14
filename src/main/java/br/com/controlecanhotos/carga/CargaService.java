package br.com.controlecanhotos.carga;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.controlecanhotos.carga.dto.AlterarStatusRequest;
import br.com.controlecanhotos.carga.dto.CargaDetalheResponse;
import br.com.controlecanhotos.carga.dto.CargaResponse;
import br.com.controlecanhotos.shared.exception.RecursoNaoEncontradoException;
import br.com.controlecanhotos.shared.paginacao.ParametrosListagem;

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

	@Transactional(readOnly = true)
	public PagedModel<CargaResponse> listar(StatusCarga status, Long coletaId, int pagina, int tamanho) {
		Specification<Carga> filtro = (root, query, cb) -> cb.conjunction();
		if (status != null) {
			filtro = filtro.and((root, query, cb) -> cb.equal(root.get("status"), status));
		}
		if (coletaId != null) {
			filtro = filtro.and((root, query, cb) -> cb.equal(root.get("coleta").get("id"), coletaId));
		}

		return new PagedModel<>(cargaRepository
				.findAll(filtro, ParametrosListagem.paginar(pagina, tamanho, Sort.by("id")))
				.map(CargaResponse::de));
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
