package br.com.controlecanhotos.carga.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import br.com.controlecanhotos.carga.Carga;
import br.com.controlecanhotos.carga.StatusCarga;
import br.com.controlecanhotos.historico.HistoricoStatus;
import br.com.controlecanhotos.notafiscal.dto.NotaFiscalResponse;

public record CargaDetalheResponse(
		Long id,
		Long coletaId,
		String chaveAcesso,
		boolean chaveProvisoria,
		String cidadeDestino,
		String ufDestino,
		StatusCarga status,
		BigDecimal pesoTotalKg,
		List<NotaFiscalResponse> notasFiscais,
		List<Historico> historico) {

	public record Historico(StatusCarga statusAnterior, StatusCarga statusNovo,
			OffsetDateTime alteradoEm, String motivo, String alteradoPor) {

		static Historico de(HistoricoStatus h) {
			return new Historico(h.getStatusAnterior(), h.getStatusNovo(), h.getAlteradoEm(), h.getMotivo(),
					h.getAlteradoPor());
		}
	}

	public static CargaDetalheResponse de(Carga carga) {
		return new CargaDetalheResponse(carga.getId(), carga.getColeta().getId(), carga.getChaveAcesso(),
				carga.isChaveProvisoria(), carga.getCidadeDestino(), carga.getUfDestino(), carga.getStatus(),
				carga.getPesoTotalKg(), carga.getNotasFiscais().stream().map(NotaFiscalResponse::de).toList(),
				carga.getHistorico().stream().map(Historico::de).toList());
	}
}
