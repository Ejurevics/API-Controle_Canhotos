package br.com.controlecanhotos;

import java.math.BigDecimal;

import br.com.controlecanhotos.carga.Carga;
import br.com.controlecanhotos.carga.ChaveAcessoCte;
import br.com.controlecanhotos.coleta.Coleta;

/** Atalhos para montar CT-es nos testes. */
public final class Cte {

	public static final String CNPJ_PADRAO = "11111111000111";

	private Cte() {
	}

	/**
	 * Chave de acesso de CT-e válida derivada de um número (usado como número do CT-e), para os testes
	 * ficarem legíveis: UF 35, emissão 10/2026, emitente {@link #CNPJ_PADRAO}, modelo 57, série 1.
	 */
	public static String chave(int numero) {
		String base = "35" + "2610" + CNPJ_PADRAO + ChaveAcessoCte.MODELO_CTE + "001" + "%09d".formatted(numero)
				+ "1" + "%08d".formatted(numero);
		return base + ChaveAcessoCte.digitoVerificador(base);
	}

	/** CT-e com uma única NF de mesmo número, emitente padrão e série 1. */
	public static Carga cargaComUmaNota(Coleta coleta, int numero) {
		Carga carga = coleta.adicionarCarga(chave(numero), "Campinas", "SP");
		carga.adicionarNotaFiscal(CNPJ_PADRAO, (short) 1, numero, BigDecimal.TEN);
		return carga;
	}
}
