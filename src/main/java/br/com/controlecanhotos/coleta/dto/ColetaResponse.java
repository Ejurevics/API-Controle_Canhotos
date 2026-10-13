package br.com.controlecanhotos.coleta.dto;

import java.time.LocalDate;
import java.util.List;

import br.com.controlecanhotos.carga.dto.CargaResponse;
import br.com.controlecanhotos.coleta.Coleta;
import br.com.controlecanhotos.coleta.SituacaoColeta;

public record ColetaResponse(
		Long id,
		String clienteNome,
		String clienteCnpj,
		LocalDate dataProgramada,
		SituacaoColeta situacao,
		List<CargaResponse> cargas) {

	public static ColetaResponse de(Coleta coleta) {
		return new ColetaResponse(coleta.getId(), coleta.getClienteNome(), coleta.getClienteCnpj(),
				coleta.getDataProgramada(), coleta.getSituacao(),
				coleta.getCargas().stream().map(CargaResponse::de).toList());
	}
}
