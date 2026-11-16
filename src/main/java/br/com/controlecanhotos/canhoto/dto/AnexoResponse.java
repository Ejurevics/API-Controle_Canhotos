package br.com.controlecanhotos.canhoto.dto;

import java.time.OffsetDateTime;

import br.com.controlecanhotos.canhoto.AnexoCanhoto;

/** Dados de um anexo do canhoto, sem o conteúdo; o arquivo sai em {@code .../anexos/{id}/arquivo}. */
public record AnexoResponse(Long id, String nomeArquivo, String tipoConteudo, int tamanhoBytes,
		OffsetDateTime enviadoEm, String enviadoPor) {

	public static AnexoResponse de(AnexoCanhoto anexo) {
		return new AnexoResponse(anexo.getId(), anexo.getNomeArquivo(), anexo.getTipo().getTipoConteudo(),
				anexo.getTamanhoBytes(), anexo.getEnviadoEm(), anexo.getEnviadoPor());
	}
}
