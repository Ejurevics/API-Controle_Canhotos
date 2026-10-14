package br.com.controlecanhotos.canhoto.dto;

import java.time.LocalDate;

import br.com.controlecanhotos.canhoto.Canhoto;
import br.com.controlecanhotos.canhoto.StatusCanhoto;
import br.com.controlecanhotos.carga.Carga;
import br.com.controlecanhotos.coleta.Coleta;

public record CanhotoResumoResponse(
		Long id,
		StatusCanhoto status,
		LocalDate dataRecebimento,
		Long cargaId,
		String emitenteCnpj,
		short serieNotaFiscal,
		int numeroNotaFiscal,
		Long coletaId,
		String clienteNome,
		String clienteCnpj,
		LocalDate dataProgramada) {

	public static CanhotoResumoResponse de(Canhoto canhoto) {
		Carga carga = canhoto.getCarga();
		Coleta coleta = carga.getColeta();
		return new CanhotoResumoResponse(canhoto.getId(), canhoto.getStatus(), canhoto.getDataRecebimento(),
				carga.getId(), carga.getEmitenteCnpj(), carga.getSerieNotaFiscal(), carga.getNumeroNotaFiscal(),
				coleta.getId(), coleta.getClienteNome(), coleta.getClienteCnpj(), coleta.getDataProgramada());
	}
}
