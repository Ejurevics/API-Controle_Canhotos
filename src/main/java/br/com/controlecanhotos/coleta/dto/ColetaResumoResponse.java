package br.com.controlecanhotos.coleta.dto;

import java.time.LocalDate;

import br.com.controlecanhotos.coleta.Coleta;
import br.com.controlecanhotos.coleta.SituacaoColeta;

public record ColetaResumoResponse(
		Long id,
		String clienteNome,
		String clienteCnpj,
		LocalDate dataProgramada,
		SituacaoColeta situacao) {

	public static ColetaResumoResponse de(Coleta coleta) {
		return new ColetaResumoResponse(coleta.getId(), coleta.getClienteNome(), coleta.getClienteCnpj(),
				coleta.getDataProgramada(), coleta.getSituacao());
	}
}
