package br.com.controlecanhotos.canhoto.dto;

import java.time.LocalDate;

import br.com.controlecanhotos.canhoto.Canhoto;
import br.com.controlecanhotos.canhoto.StatusCanhoto;
import br.com.controlecanhotos.carga.Carga;
import br.com.controlecanhotos.coleta.Coleta;
import br.com.controlecanhotos.notafiscal.NotaFiscal;

public record CanhotoResumoResponse(
		Long id,
		StatusCanhoto status,
		LocalDate dataRecebimento,
		Long notaFiscalId,
		String emitenteCnpj,
		short serieNotaFiscal,
		int numeroNotaFiscal,
		Long cargaId,
		String chaveAcesso,
		Long coletaId,
		String clienteNome,
		String clienteCnpj,
		LocalDate dataProgramada) {

	public static CanhotoResumoResponse de(Canhoto canhoto) {
		NotaFiscal nota = canhoto.getNotaFiscal();
		Carga carga = nota.getCarga();
		Coleta coleta = carga.getColeta();
		return new CanhotoResumoResponse(canhoto.getId(), canhoto.getStatus(), canhoto.getDataRecebimento(),
				nota.getId(), nota.getEmitenteCnpj(), nota.getSerieNotaFiscal(), nota.getNumeroNotaFiscal(),
				carga.getId(), carga.getChaveAcesso(), coleta.getId(), coleta.getClienteNome(),
				coleta.getClienteCnpj(), coleta.getDataProgramada());
	}
}
