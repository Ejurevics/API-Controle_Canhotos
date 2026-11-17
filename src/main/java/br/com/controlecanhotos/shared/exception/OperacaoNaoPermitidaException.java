package br.com.controlecanhotos.shared.exception;

/** O usuário está autenticado, mas a regra de negócio não deixa ele fazer esta operação; responde 403. */
public class OperacaoNaoPermitidaException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public OperacaoNaoPermitidaException(String mensagem) {
		super(mensagem);
	}
}
