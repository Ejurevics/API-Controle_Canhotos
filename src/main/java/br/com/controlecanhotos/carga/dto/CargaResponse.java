package br.com.controlecanhotos.carga.dto;

import java.math.BigDecimal;
import java.util.List;

import br.com.controlecanhotos.carga.Carga;
import br.com.controlecanhotos.carga.StatusCarga;
import br.com.controlecanhotos.notafiscal.dto.NotaFiscalResponse;

public record CargaResponse(
		Long id,
		String chaveAcesso,
		boolean chaveProvisoria,
		String cidadeDestino,
		String ufDestino,
		StatusCarga status,
		BigDecimal pesoTotalKg,
		List<NotaFiscalResponse> notasFiscais) {

	public static CargaResponse de(Carga carga) {
		return new CargaResponse(carga.getId(), carga.getChaveAcesso(), carga.isChaveProvisoria(),
				carga.getCidadeDestino(), carga.getUfDestino(), carga.getStatus(), carga.getPesoTotalKg(),
				carga.getNotasFiscais().stream().map(NotaFiscalResponse::de).toList());
	}
}
