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
import br.com.controlecanhotos.notafiscal.NotaFiscal;
import br.com.controlecanhotos.notafiscal.NotaFiscalRepository;
import br.com.controlecanhotos.shared.exception.RecursoNaoEncontradoException;
import br.com.controlecanhotos.shared.exception.RegraNegocioException;
import br.com.controlecanhotos.shared.paginacao.ParametrosListagem;

@Service
public class CanhotoService {

	private final NotaFiscalRepository notaFiscalRepository;

	private final CanhotoRepository canhotoRepository;

	public CanhotoService(NotaFiscalRepository notaFiscalRepository, CanhotoRepository canhotoRepository) {
		this.notaFiscalRepository = notaFiscalRepository;
		this.canhotoRepository = canhotoRepository;
	}

	@Transactional(readOnly = true)
	public CanhotoResponse buscar(Long notaFiscalId) {
		return CanhotoResponse.de(obter(notaFiscalId).getCanhoto());
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
					root.get("notaFiscal").get("carga").get("coleta").get("clienteCnpj"), clienteCnpj));
		}
		if (dataInicio != null) {
			filtro = filtro.and((root, query, cb) -> cb.greaterThanOrEqualTo(
					root.get("notaFiscal").get("carga").get("coleta").get("dataProgramada"), dataInicio));
		}
		if (dataFim != null) {
			filtro = filtro.and((root, query, cb) -> cb.lessThanOrEqualTo(
					root.get("notaFiscal").get("carga").get("coleta").get("dataProgramada"), dataFim));
		}

		return new PagedModel<>(canhotoRepository
				.findAll(filtro, ParametrosListagem.paginar(pagina, tamanho, Sort.by("id")))
				.map(CanhotoResumoResponse::de));
	}

	@Transactional
	public CanhotoResponse registrar(Long notaFiscalId, RegistrarCanhotoRequest request) {
		Canhoto canhoto = obter(notaFiscalId).getCanhoto();
		boolean confirmada = Boolean.TRUE.equals(request.dataFuturaConfirmada());

		switch (request.status()) {
			case RECEBIDO -> canhoto.registrarRecebimento(request.dataRecebimento(), confirmada);
			case DIVERGENTE -> canhoto.registrarDivergencia(request.dataRecebimento(), request.observacao(),
					confirmada);
			default -> throw new RegraNegocioException(
					"Só é possível registrar o canhoto como RECEBIDO ou DIVERGENTE");
		}
		// Grava antes de montar a resposta: data e usuário do histórico novo só são preenchidos na gravação.
		canhotoRepository.flush();
		return CanhotoResponse.de(canhoto);
	}

	private NotaFiscal obter(Long notaFiscalId) {
		return notaFiscalRepository.findById(notaFiscalId)
				.orElseThrow(() -> new RecursoNaoEncontradoException(
						"Nota fiscal não encontrada: " + notaFiscalId));
	}
}
