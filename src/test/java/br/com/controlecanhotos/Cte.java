package br.com.controlecanhotos;

import java.math.BigDecimal;

import br.com.controlecanhotos.carga.Carga;
import br.com.controlecanhotos.coleta.Coleta;

/** Atalhos para montar CT-es nos testes. */
public final class Cte {

	public static final String CNPJ_PADRAO = "11111111000111";

	private Cte() {
	}

	/** Chave de acesso de 44 dígitos derivada de um número, para os testes ficarem legíveis. */
	public static String chave(int numero) {
		return "%044d".formatted(numero);
	}

	/** CT-e com uma única NF de mesmo número, emitente padrão e série 1. */
	public static Carga cargaComUmaNota(Coleta coleta, int numero) {
		Carga carga = coleta.adicionarCarga(chave(numero), "Campinas", "SP");
		carga.adicionarNotaFiscal(CNPJ_PADRAO, (short) 1, numero, BigDecimal.TEN);
		return carga;
	}
}
