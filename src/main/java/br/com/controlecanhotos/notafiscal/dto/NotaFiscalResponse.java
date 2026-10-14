package br.com.controlecanhotos.notafiscal.dto;

import java.math.BigDecimal;

import br.com.controlecanhotos.canhoto.StatusCanhoto;
import br.com.controlecanhotos.notafiscal.NotaFiscal;

public record NotaFiscalResponse(
		Long id,
		String emitenteCnpj,
		short serieNotaFiscal,
		int numeroNotaFiscal,
		BigDecimal pesoKg,
		StatusCanhoto statusCanhoto) {

	public static NotaFiscalResponse de(NotaFiscal nota) {
		return new NotaFiscalResponse(nota.getId(), nota.getEmitenteCnpj(), nota.getSerieNotaFiscal(),
				nota.getNumeroNotaFiscal(), nota.getPesoKg(), nota.getCanhoto().getStatus());
	}
}
