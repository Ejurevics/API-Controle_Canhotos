package br.com.controlecanhotos.coleta.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import br.com.controlecanhotos.carga.dto.CargaResponse;
import br.com.controlecanhotos.coleta.Coleta;
import br.com.controlecanhotos.coleta.SituacaoColeta;
import br.com.controlecanhotos.historico.HistoricoSituacaoColeta;

public record ColetaResponse(
		Long id,
		String clienteNome,
		String clienteCnpj,
		LocalDate dataProgramada,
		SituacaoColeta situacao,
		List<CargaResponse> cargas,
		List<Historico> historico) {

	public record Historico(SituacaoColeta situacaoAnterior, SituacaoColeta situacaoNova,
			OffsetDateTime alteradoEm, String alteradoPor) {

		static Historico de(HistoricoSituacaoColeta h) {
			return new Historico(h.getSituacaoAnterior(), h.getSituacaoNova(), h.getAlteradoEm(),
					h.getAlteradoPor());
		}
	}

	public static ColetaResponse de(Coleta coleta) {
		return new ColetaResponse(coleta.getId(), coleta.getClienteNome(), coleta.getClienteCnpj(),
				coleta.getDataProgramada(), coleta.getSituacao(),
				coleta.getCargas().stream().map(CargaResponse::de).toList(),
				coleta.getHistorico().stream().map(Historico::de).toList());
	}
}
