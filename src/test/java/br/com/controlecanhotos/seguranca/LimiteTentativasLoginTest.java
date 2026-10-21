package br.com.controlecanhotos.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import br.com.controlecanhotos.RelogioManual;
import br.com.controlecanhotos.shared.exception.TentativasExcedidasException;

class LimiteTentativasLoginTest {

	private static final String IP = "10.0.0.1";

	private final RelogioManual relogio = new RelogioManual();

	/** 3 falhas por login, 5 por IP, janela de 15 minutos. */
	private final LimiteTentativasLogin limite = new LimiteTentativasLogin(
			new PropriedadesSeguranca.Login(3, 5, Duration.ofMinutes(15)), relogio);

	private void falhar(String login, String ip, int vezes) {
		for (int i = 0; i < vezes; i++) {
			limite.verificar(login, ip);
			limite.registrarFalha(login, ip);
		}
	}

	@Test
	void bloqueiaOLoginAoAtingirOLimiteAteAJanelaAcabar() {
		falhar("maria", IP, 3);
		relogio.avancar(Duration.ofMinutes(5));

		assertThatThrownBy(() -> limite.verificar("maria", IP))
				.isInstanceOfSatisfying(TentativasExcedidasException.class,
						ex -> assertThat(ex.getEsperaEmSegundos()).isEqualTo(600));

		relogio.avancar(Duration.ofMinutes(10));
		assertThatCode(() -> limite.verificar("maria", IP)).doesNotThrowAnyException();
	}

	@Test
	void bloqueioDoLoginValeDeQualquerIp() {
		falhar("maria", IP, 3);

		assertThatThrownBy(() -> limite.verificar("maria", "10.0.0.2"))
				.isInstanceOf(TentativasExcedidasException.class);
		assertThatCode(() -> limite.verificar("joao", "10.0.0.2")).doesNotThrowAnyException();
	}

	@Test
	void bloqueiaOIpQueErraEmVariosLogins() {
		for (String login : new String[] { "a", "b", "c", "d", "e" }) {
			falhar(login, IP, 1);
		}

		assertThatThrownBy(() -> limite.verificar("outro", IP)).isInstanceOf(TentativasExcedidasException.class);
		assertThatCode(() -> limite.verificar("outro", "10.0.0.2")).doesNotThrowAnyException();
	}

	@Test
	void sucessoZeraOLoginMasNaoOIp() {
		falhar("maria", IP, 2);
		limite.registrarSucesso("maria");
		falhar("maria", IP, 2);

		assertThatCode(() -> limite.verificar("maria", IP)).doesNotThrowAnyException();

		falhar("joao", IP, 1);
		assertThatThrownBy(() -> limite.verificar("joao", IP))
				.as("o IP já soma 5 falhas, mesmo com o sucesso de maria")
				.isInstanceOf(TentativasExcedidasException.class);
	}

	@Test
	void falhasEspalhadasNaoBloqueiamQuandoAJanelaVence() {
		falhar("maria", IP, 2);
		relogio.avancar(Duration.ofMinutes(15));
		falhar("maria", IP, 2);

		assertThatCode(() -> limite.verificar("maria", IP)).doesNotThrowAnyException();
	}

	@Test
	void esperaInformadaEhAMaiorEntreLoginEIp() {
		falhar("maria", "10.0.0.2", 3); // janela do login começa agora
		relogio.avancar(Duration.ofMinutes(10));
		for (String login : new String[] { "a", "b", "c", "d", "e" }) {
			falhar(login, IP, 1); // janela do IP começa 10 minutos depois
		}

		assertThatThrownBy(() -> limite.verificar("maria", IP))
				.isInstanceOfSatisfying(TentativasExcedidasException.class,
						ex -> assertThat(ex.getEsperaEmSegundos()).isEqualTo(900));
	}
}
