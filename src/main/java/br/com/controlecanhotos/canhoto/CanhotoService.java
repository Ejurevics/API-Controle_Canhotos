package br.com.controlecanhotos.canhoto;

import java.time.LocalDate;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.controlecanhotos.canhoto.dto.CanhotoResponse;
import br.com.controlecanhotos.canhoto.dto.CanhotoResumoResponse;
import br.com.controlecanhotos.canhoto.dto.RegistrarCanhotoRequest;
import br.com.controlecanhotos.carga.Carga;
import br.com.controlecanhotos.carga.CargaRepository;
import br.com.controlecanhotos.shared.exception.RecursoNaoEncontradoException;
import br.com.controlecanhotos.shared.exception.RegraNegocioException;
import br.com.controlecanhotos.shared.paginacao.ParametrosListagem;

@Service
public class CanhotoService {

	private final CargaRepository cargaRepository;

	private final CanhotoRepository canhotoRepository;

	public CanhotoService(CargaRepository cargaRepository, CanhotoRepository canhotoRepository) {
		this.cargaRepository = cargaRepository;
		this.canhotoRepository = canhotoRepository;
	}

	@Transactional(readOnly = true)
	public CanhotoResponse buscar(Long cargaId) {
		return CanhotoResponse.de(obter(cargaId).getCanhoto());
	}

	@Transactional(readOnly = true)
	public PagedModel<CanhotoResumoResponse> listar(StatusCanhoto status, String clienteCnpj,
			LocalDate dataInicio, LocalDate dataFim, int pagina, int tamanho) {
		ParametrosListagem.validarPeriodo(dataInicio, dataFim);

		Specification<Canhoto> filtro = (root, query, cb) -> cb.conjunction();
		if (status != null) {
			filtro = filtro.and((root, query, cb) -> cb.equal(root.get("status"), status));
		}
		if (clienteCnpj != null) {
			filtro = filtro.and((root, query, cb) -> cb.equal(
					root.get("carga").get("coleta").get("clienteCnpj"), clienteCnpj));
		}
		if (dataInicio != null) {
			filtro = filtro.and((root, query, cb) -> cb.greaterThanOrEqualTo(
					root.get("carga").get("coleta").get("dataProgramada"), dataInicio));
		}
		if (dataFim != null) {
			filtro = filtro.and((root, query, cb) -> cb.lessThanOrEqualTo(
					root.get("carga").get("coleta").get("dataProgramada"), dataFim));
		}

		return new PagedModel<>(canhotoRepository
				.findAll(filtro, ParametrosListagem.paginar(pagina, tamanho, Sort.by("id")))
				.map(CanhotoResumoResponse::de));
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
