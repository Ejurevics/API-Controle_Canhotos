package br.com.controlecanhotos.shared.exception;

import java.time.Duration;

public class TentativasExcedidasException extends RuntimeException {

	private final Duration espera;

	public TentativasExcedidasException(Duration espera) {
		super("Muitas tentativas de login. Tente novamente em %d segundos".formatted(segundos(espera)));
		this.espera = espera;
	}

	/** Arredonda para cima, para o cliente não tentar um instante antes do desbloqueio. */
	public long getEsperaEmSegundos() {
		return segundos(espera);
	}

	private static long segundos(Duration espera) {
		return Math.max(1, espera.plusMillis(999).toSeconds());
	}
}
