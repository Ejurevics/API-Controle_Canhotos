package br.com.controlecanhotos.carga.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import br.com.controlecanhotos.canhoto.StatusCanhoto;
import br.com.controlecanhotos.carga.Carga;
import br.com.controlecanhotos.carga.StatusCarga;
import br.com.controlecanhotos.historico.HistoricoStatus;

public record CargaDetalheResponse(
		Long id,
		Long coletaId,
		String emitenteCnpj,
		short serieNotaFiscal,
		int numeroNotaFiscal,
		BigDecimal pesoKg,
		String cidadeDestino,
		String ufDestino,
		StatusCarga status,
		StatusCanhoto statusCanhoto,
		List<Historico> historico) {

	public record Historico(StatusCarga statusAnterior, StatusCarga statusNovo,
			OffsetDateTime alteradoEm, String motivo) {

		static Historico de(HistoricoStatus h) {
			return new Historico(h.getStatusAnterior(), h.getStatusNovo(), h.getAlteradoEm(), h.getMotivo());
		}
	}

	public static CargaDetalheResponse de(Carga carga) {
		return new CargaDetalheResponse(carga.getId(), carga.getColeta().getId(), carga.getEmitenteCnpj(),
				carga.getSerieNotaFiscal(), carga.getNumeroNotaFiscal(), carga.getPesoKg(),
				carga.getCidadeDestino(), carga.getUfDestino(), carga.getStatus(),
				carga.getCanhoto().getStatus(), carga.getHistorico().stream().map(Historico::de).toList());
	}
}
