package br.com.controlecanhotos.carga.dto;

import br.com.controlecanhotos.carga.Carga;
import br.com.controlecanhotos.carga.StatusCarga;

/** Item das listagens: traz só contagens, sem carregar as notas fiscais de cada carga. */
public record CargaResumoResponse(
		Long id,
		Long coletaId,
		String chaveAcesso,
		boolean chaveProvisoria,
		String cidadeDestino,
		String ufDestino,
		StatusCarga status,
		int quantidadeNotasFiscais,
		int canhotosPendentes) {

	public static CargaResumoResponse de(Carga carga) {
		return new CargaResumoResponse(carga.getId(), carga.getColeta().getId(), carga.getChaveAcesso(),
				carga.isChaveProvisoria(), carga.getCidadeDestino(), carga.getUfDestino(), carga.getStatus(),
				carga.getQuantidadeNotasFiscais(), carga.getCanhotosPendentes());
	}
}
