package br.com.controlecanhotos.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SegurancaConfigTest {

	private static PropriedadesSeguranca comSegredo(String segredo) {
		return new PropriedadesSeguranca(new PropriedadesSeguranca.Jwt(segredo, Duration.ofHours(1)), null);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "curto", "1234567890123456789012345678901" })
	void recusaSegredoAusenteOuComMenosDe32Bytes(String segredo) {
		assertThatThrownBy(() -> new SegurancaConfig().chaveJwt(comSegredo(segredo)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("JWT_SECRET");
	}

	@Test
	void aceitaSegredoCom32Bytes() {
		assertThat(new SegurancaConfig().chaveJwt(comSegredo("12345678901234567890123456789012")).getAlgorithm())
				.isEqualTo("HmacSHA256");
	}
}
