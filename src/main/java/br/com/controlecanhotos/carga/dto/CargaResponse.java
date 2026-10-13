package br.com.controlecanhotos.carga.dto;

import java.math.BigDecimal;

import br.com.controlecanhotos.canhoto.StatusCanhoto;
import br.com.controlecanhotos.carga.Carga;
import br.com.controlecanhotos.carga.StatusCarga;

public record CargaResponse(
		Long id,
		String emitenteCnpj,
		short serieNotaFiscal,
		int numeroNotaFiscal,
		BigDecimal pesoKg,
		String cidadeDestino,
		String ufDestino,
		StatusCarga status,
		StatusCanhoto statusCanhoto) {

	public static CargaResponse de(Carga carga) {
		return new CargaResponse(carga.getId(), carga.getEmitenteCnpj(), carga.getSerieNotaFiscal(),
				carga.getNumeroNotaFiscal(), carga.getPesoKg(), carga.getCidadeDestino(),
				carga.getUfDestino(), carga.getStatus(), carga.getCanhoto().getStatus());
	}
}
