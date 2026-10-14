package br.com.controlecanhotos.carga;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ChaveAcessoCteTest {

	// Dígitos verificadores calculados fora do Java (awk, módulo 11), como referência independente.
	@ParameterizedTest
	@ValueSource(strings = {
			"35261011111111000111570010000000011000000012",
			"35261011111111000111570010000012341000012343",
			"41261055555555000155570010000987651000123450" })
	void aceitaChavesValidas(String chave) {
		assertThat(ChaveAcessoCte.problema(chave)).isEmpty();
	}

	@Test
	void restoZeroOuUmViraDigitoZero() {
		assertThat(ChaveAcessoCte.digitoVerificador("4126105555555500015557001000098765100012345")).isZero();
	}

	@Test
	void rejeitaDigitoVerificadorErrado() {
		assertThat(ChaveAcessoCte.problema("35261011111111000111570010000012341000012345"))
				.contains("dígito verificador inválido");
	}

	@Test
	void rejeitaChaveDeOutroModelo() {
		String nfe = "3526101111111100011155001000001234100001234";
		String chave = nfe + ChaveAcessoCte.digitoVerificador(nfe);

		assertThat(ChaveAcessoCte.problema(chave)).contains("não é de CT-e: modelo 55, esperado 57");
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "123", "3526101111111100011157001000001234100001234",
			"352610111111110001115700100000123410000123435", "3526101111111100011157001000001234100001234X" })
	void rejeitaTamanhoOuCaractereInvalido(String chave) {
		assertThat(ChaveAcessoCte.problema(chave)).contains("deve ter 44 dígitos");
	}

	@Test
	void rejeitaNulo() {
		assertThat(ChaveAcessoCte.problema(null)).contains("deve ter 44 dígitos");
	}
}
