package br.com.controlecanhotos.carga;

import java.util.EnumSet;
import java.util.Set;

public enum StatusCarga {
	AGUARDANDO_COLETA,
	COLETADA,
	EM_TRANSITO,
	ENTREGUE,
	OCORRENCIA,
	DEVOLVIDA;

	public Set<StatusCarga> proximosPermitidos() {
		return switch (this) {
			case AGUARDANDO_COLETA -> EnumSet.of(COLETADA, OCORRENCIA);
			case COLETADA -> EnumSet.of(EM_TRANSITO, OCORRENCIA);
			case EM_TRANSITO -> EnumSet.of(ENTREGUE, OCORRENCIA, DEVOLVIDA);
			case OCORRENCIA -> EnumSet.of(AGUARDANDO_COLETA, COLETADA, EM_TRANSITO, ENTREGUE, DEVOLVIDA);
			case ENTREGUE, DEVOLVIDA -> EnumSet.noneOf(StatusCarga.class);
		};
	}

	public boolean podeIrPara(StatusCarga novo) {
		return proximosPermitidos().contains(novo);
	}
}
