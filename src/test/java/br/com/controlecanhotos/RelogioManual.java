package br.com.controlecanhotos;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Relógio que o teste avança à mão. */
public final class RelogioManual extends Clock {

	private Instant agora = Instant.parse("2026-10-14T12:00:00Z");

	public void avancar(Duration duracao) {
		agora = agora.plus(duracao);
	}

	@Override
	public Instant instant() {
		return agora;
	}

	@Override
	public ZoneId getZone() {
		return ZoneOffset.UTC;
	}

	@Override
	public Clock withZone(ZoneId zone) {
		return this;
	}
}
