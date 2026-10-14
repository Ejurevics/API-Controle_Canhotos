package br.com.controlecanhotos.carga;

import java.util.Optional;

/**
 * Regras da chave de acesso do CT-e: 44 dígitos, modelo 57 nas posições 21 e 22 e dígito verificador
 * (módulo 11, pesos de 2 a 9 da direita para a esquerda) na última posição.
 */
public final class ChaveAcessoCte {

	public static final String MODELO_CTE = "57";

	private ChaveAcessoCte() {
	}

	/** Motivo pelo qual a chave é inválida, ou vazio se ela for válida. */
	public static Optional<String> problema(String chave) {
		if (chave == null || !chave.matches("\\d{44}")) {
			return Optional.of("deve ter 44 dígitos");
		}
		String modelo = chave.substring(20, 22);
		if (!MODELO_CTE.equals(modelo)) {
			return Optional.of("não é de CT-e: modelo %s, esperado %s".formatted(modelo, MODELO_CTE));
		}
		int esperado = digitoVerificador(chave.substring(0, 43));
		if (chave.charAt(43) - '0' != esperado) {
			return Optional.of("dígito verificador inválido");
		}
		return Optional.empty();
	}

	/** Dígito verificador dos 43 primeiros dígitos da chave. */
	public static int digitoVerificador(String base) {
		int soma = 0;
		int peso = 2;
		for (int i = base.length() - 1; i >= 0; i--) {
			soma += (base.charAt(i) - '0') * peso;
			peso = peso == 9 ? 2 : peso + 1;
		}
		int resto = soma % 11;
		return resto < 2 ? 0 : 11 - resto;
	}
}
