package br.com.controlecanhotos.shared.exception;

/** Arquivo enviado acima do limite configurado; responde 413. */
public class ArquivoMuitoGrandeException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ArquivoMuitoGrandeException(String mensagem) {
		super(mensagem);
	}
}
