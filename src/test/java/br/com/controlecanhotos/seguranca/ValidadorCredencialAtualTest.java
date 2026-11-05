package br.com.controlecanhotos.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import br.com.controlecanhotos.usuario.Perfil;
import br.com.controlecanhotos.usuario.Usuario;
import br.com.controlecanhotos.usuario.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class ValidadorCredencialAtualTest {

	@Mock
	private UsuarioRepository usuarioRepository;

	private ValidadorCredencialAtual validador;

	@BeforeEach
	void criarValidador() {
		validador = new ValidadorCredencialAtual(usuarioRepository, new CacheCredenciais(Duration.ZERO, Clock.systemUTC()));
	}

	private static Jwt token(Object versao) {
		Jwt.Builder builder = Jwt.withTokenValue("t").header("alg", "HS256").subject("maria");
		if (versao != null) {
			builder.claim(SegurancaConfig.CLAIM_VERSAO_CREDENCIAL, versao);
		}
		return builder.build();
	}

	private static Usuario maria() {
		return new Usuario("maria", "Maria", "hash", Perfil.OPERADOR);
	}

	@Test
	void aceitaTokenComAVersaoAtual() {
		when(usuarioRepository.findByLogin("maria")).thenReturn(Optional.of(maria()));

		assertThat(validador.validate(token(0L)).hasErrors()).isFalse();
	}

	@Test
	void recusaTokenEmitidoAntesDaTrocaDeSenha() {
		Usuario maria = maria();
		maria.redefinirSenha("hash-novo");
		when(usuarioRepository.findByLogin("maria")).thenReturn(Optional.of(maria));

		assertThat(validador.validate(token(0L)).hasErrors()).isTrue();
		assertThat(validador.validate(token(1L)).hasErrors()).isFalse();
	}

	@Test
	void recusaUsuarioDesativadoMesmoDepoisDeReativado() {
		Usuario maria = maria();
		maria.desativar();
		when(usuarioRepository.findByLogin("maria")).thenReturn(Optional.of(maria));

		assertThat(validador.validate(token(1L)).hasErrors()).as("inativo").isTrue();

		maria.ativar();
		assertThat(validador.validate(token(0L)).hasErrors()).as("token de antes da desativação").isTrue();
		assertThat(validador.validate(token(1L)).hasErrors()).as("token novo").isFalse();
	}

	@Test
	void recusaUsuarioQueNaoExiste() {
		when(usuarioRepository.findByLogin("maria")).thenReturn(Optional.empty());

		assertThat(validador.validate(token(0L)).hasErrors()).isTrue();
	}

	@Test
	void recusaTokenSemVersaoSemConsultarOBanco() {
		assertThat(validador.validate(token(null)).hasErrors()).isTrue();
		assertThat(validador.validate(token("0")).hasErrors()).isTrue();
		verifyNoInteractions(usuarioRepository);
	}
}
