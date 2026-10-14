package br.com.controlecanhotos.canhoto.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import br.com.controlecanhotos.canhoto.Canhoto;
import br.com.controlecanhotos.canhoto.StatusCanhoto;
import br.com.controlecanhotos.historico.HistoricoCanhoto;

public record CanhotoResponse(
		Long id,
		Long notaFiscalId,
		StatusCanhoto status,
		LocalDate dataRecebimento,
		String observacao,
		List<Registro> historico) {

	public record Registro(StatusCanhoto statusAnterior, StatusCanhoto statusNovo, LocalDate dataRecebimento,
			String observacao, boolean dataFuturaConfirmada, OffsetDateTime registradoEm) {

		static Registro de(HistoricoCanhoto h) {
			return new Registro(h.getStatusAnterior(), h.getStatusNovo(), h.getDataRecebimento(),
					h.getObservacao(), h.isDataFuturaConfirmada(), h.getRegistradoEm());
		}
	}

	public static CanhotoResponse de(Canhoto canhoto) {
		return new CanhotoResponse(canhoto.getId(), canhoto.getNotaFiscal().getId(), canhoto.getStatus(),
				canhoto.getDataRecebimento(), canhoto.getObservacao(),
				canhoto.getHistorico().stream().map(Registro::de).toList());
	}
}
