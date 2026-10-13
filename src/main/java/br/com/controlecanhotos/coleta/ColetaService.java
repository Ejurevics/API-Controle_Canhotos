package br.com.controlecanhotos.coleta;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.controlecanhotos.carga.CargaRepository;
import br.com.controlecanhotos.carga.dto.CriarCargaRequest;
import br.com.controlecanhotos.coleta.dto.ColetaResponse;
import br.com.controlecanhotos.coleta.dto.CriarColetaRequest;
import br.com.controlecanhotos.shared.exception.RecursoNaoEncontradoException;
import br.com.controlecanhotos.shared.exception.RegraNegocioException;

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
		Set<String> notasDaRequisicao = new HashSet<>();

		for (CriarCargaRequest item : request.cargas()) {
			String emitenteCnpj = item.emitenteCnpj() != null ? item.emitenteCnpj() : request.clienteCnpj();
			short serie = item.serieNotaFiscal() != null ? item.serieNotaFiscal() : SERIE_PADRAO;
			int numero = item.numeroNotaFiscal();
			String descricaoNf = "%s/%d/%d".formatted(emitenteCnpj, serie, numero);

			if (!notasDaRequisicao.add(descricaoNf)) {
				throw new RegraNegocioException("Nota fiscal repetida na requisição: " + descricaoNf);
			}
			if (cargaRepository.existsByEmitenteCnpjAndSerieNotaFiscalAndNumeroNotaFiscal(
					emitenteCnpj, serie, numero)) {
				throw new RegraNegocioException("Nota fiscal já cadastrada: " + descricaoNf);
			}
			coleta.adicionarCarga(emitenteCnpj, serie, numero, item.pesoKg(), item.cidadeDestino(),
					item.ufDestino());
		}
		return ColetaResponse.de(coletaRepository.save(coleta));
	}

	@Transactional(readOnly = true)
	public ColetaResponse buscar(Long id) {
		return coletaRepository.findById(id)
				.map(ColetaResponse::de)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Coleta não encontrada: " + id));
	}

	@Transactional
	public ColetaResponse alterarSituacao(Long id, SituacaoColeta novaSituacao) {
		Coleta coleta = coletaRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Coleta não encontrada: " + id));
		coleta.alterarSituacao(novaSituacao);
		return ColetaResponse.de(coleta);
	}
}
