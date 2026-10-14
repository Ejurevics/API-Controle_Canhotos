package br.com.controlecanhotos.coleta;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.controlecanhotos.carga.Carga;
import br.com.controlecanhotos.carga.CargaRepository;
import br.com.controlecanhotos.carga.dto.CriarCargaRequest;
import br.com.controlecanhotos.coleta.dto.ColetaResponse;
import br.com.controlecanhotos.coleta.dto.ColetaResumoResponse;
import br.com.controlecanhotos.coleta.dto.CriarColetaRequest;
import br.com.controlecanhotos.notafiscal.dto.CriarNotaFiscalRequest;
import br.com.controlecanhotos.shared.exception.RecursoNaoEncontradoException;
import br.com.controlecanhotos.shared.exception.RegraNegocioException;
import br.com.controlecanhotos.shared.paginacao.ParametrosListagem;

@Service
public class ColetaService {

	private static final short SERIE_PADRAO = 1;

	private final ColetaRepository coletaRepository;
	private final CargaRepository cargaRepository;

	public ColetaService(ColetaRepository coletaRepository, CargaRepository cargaRepository) {
		this.coletaRepository = coletaRepository;
		this.cargaRepository = cargaRepository;
	}

	@Transactional
	public ColetaResponse criar(CriarColetaRequest request) {
		Coleta coleta = new Coleta(request.clienteNome(), request.clienteCnpj(), request.dataProgramada());
		Set<String> chavesDaRequisicao = new HashSet<>();

		for (CriarCargaRequest item : request.cargas()) {
			String chave = item.chaveAcesso();
			if (!chavesDaRequisicao.add(chave)) {
				throw new RegraNegocioException("CT-e repetido na requisição: " + chave);
			}
			if (cargaRepository.existsByChaveAcesso(chave)) {
				throw new RegraNegocioException("CT-e já cadastrado: " + chave);
			}

			Carga carga = coleta.adicionarCarga(chave, item.cidadeDestino(), item.ufDestino());
			for (CriarNotaFiscalRequest nota : item.notasFiscais()) {
				String emitenteCnpj = nota.emitenteCnpj() != null ? nota.emitenteCnpj() : request.clienteCnpj();
				short serie = nota.serieNotaFiscal() != null ? nota.serieNotaFiscal() : SERIE_PADRAO;
				carga.adicionarNotaFiscal(emitenteCnpj, serie, nota.numeroNotaFiscal(), nota.pesoKg());
			}
		}
		return ColetaResponse.de(coletaRepository.save(coleta));
	}

	@Transactional(readOnly = true)
	public ColetaResponse buscar(Long id) {
		return coletaRepository.findById(id)
				.map(ColetaResponse::de)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Coleta não encontrada: " + id));
	}

	@Transactional(readOnly = true)
	public PagedModel<ColetaResumoResponse> listar(SituacaoColeta situacao, String clienteCnpj,
			LocalDate dataInicio, LocalDate dataFim, int pagina, int tamanho) {
		ParametrosListagem.validarPeriodo(dataInicio, dataFim);

		Specification<Coleta> filtro = (root, query, cb) -> cb.conjunction();
		if (situacao != null) {
			filtro = filtro.and((root, query, cb) -> cb.equal(root.get("situacao"), situacao));
		}
		if (clienteCnpj != null) {
			filtro = filtro.and((root, query, cb) -> cb.equal(root.get("clienteCnpj"), clienteCnpj));
		}
		if (dataInicio != null) {
			filtro = filtro.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dataProgramada"), dataInicio));
		}
		if (dataFim != null) {
			filtro = filtro.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("dataProgramada"), dataFim));
		}

		Sort ordenacao = Sort.by(Sort.Order.desc("dataProgramada"), Sort.Order.desc("id"));
		return new PagedModel<>(coletaRepository
				.findAll(filtro, ParametrosListagem.paginar(pagina, tamanho, ordenacao))
				.map(ColetaResumoResponse::de));
	}

	@Transactional
	public ColetaResponse alterarSituacao(Long id, SituacaoColeta novaSituacao) {
		Coleta coleta = coletaRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Coleta não encontrada: " + id));
		coleta.alterarSituacao(novaSituacao);
		// Grava antes de montar a resposta: data e usuário do histórico novo só são preenchidos na gravação.
		coletaRepository.flush();
		return ColetaResponse.de(coleta);
	}
}
