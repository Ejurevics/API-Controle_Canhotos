package br.com.controlecanhotos.canhoto.dto;

import br.com.controlecanhotos.canhoto.TipoAnexo;

/** Arquivo de um anexo, para o download. */
public record ArquivoAnexo(String nomeArquivo, TipoAnexo tipo, byte[] conteudo) {
}
