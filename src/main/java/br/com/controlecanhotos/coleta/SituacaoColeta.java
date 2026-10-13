package br.com.controlecanhotos.coleta;

import java.util.EnumSet;
import java.util.Set;

public enum SituacaoColeta {
	PROGRAMADA,
	EM_ANDAMENTO,
	CONCLUIDA,
	CANCELADA;

	public Set<SituacaoColeta> proximasPermitidas() {
		return switch (this) {
			case PROGRAMADA -> EnumSet.of(EM_ANDAMENTO, CANCELADA);
			case EM_ANDAMENTO -> EnumSet.of(CONCLUIDA, CANCELADA);
			case CONCLUIDA, CANCELADA -> EnumSet.noneOf(SituacaoColeta.class);
		};
	}

	public boolean podeIrPara(SituacaoColeta nova) {
		return proximasPermitidas().contains(nova);
	}
}
