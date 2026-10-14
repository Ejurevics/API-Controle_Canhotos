package br.com.controlecanhotos.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.controlecanhotos.usuario.Perfil;
import br.com.controlecanhotos.usuario.Usuario;
import br.com.controlecanhotos.usuario.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AdminInicialTest {

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	private AdminInicial adminInicial(String login, String senha) {
		PropriedadesSeguranca propriedades = new PropriedadesSeguranca(
				new PropriedadesSeguranca.Jwt("x".repeat(32), Duration.ofHours(1)),
				new PropriedadesSeguranca.AdminInicial(login, null, senha), null);
		return new AdminInicial(usuarioRepository, passwordEncoder, propriedades);
	}

	@Test
	void criaAdminQuandoNaoExisteNenhum() {
		when(usuarioRepository.existsByPerfil(Perfil.ADMIN)).thenReturn(false);
		when(passwordEncoder.encode("senha-forte")).thenReturn("hash");

		adminInicial(" Admin ", "senha-forte").run(null);

		ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
		verify(usuarioRepository).save(salvo.capture());
		assertThat(salvo.getValue().getLogin()).isEqualTo("admin");
		assertThat(salvo.getValue().getNome()).isEqualTo("Administrador");
		assertThat(salvo.getValue().getSenhaHash()).isEqualTo("hash");
		assertThat(salvo.getValue().getPerfil()).isEqualTo(Perfil.ADMIN);
		assertThat(salvo.getValue().isAtivo()).isTrue();
	}

	@Test
	void naoCriaQuandoJaExisteAdmin() {
		when(usuarioRepository.existsByPerfil(Perfil.ADMIN)).thenReturn(true);

		adminInicial("admin", "senha-forte").run(null);

		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void semSenhaConfiguradaSoAvisaENaoCria() {
		when(usuarioRepository.existsByPerfil(Perfil.ADMIN)).thenReturn(false);

		adminInicial("admin", " ").run(null);

		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void recusaSenhaCurta() {
		when(usuarioRepository.existsByPerfil(Perfil.ADMIN)).thenReturn(false);

		assertThatThrownBy(() -> adminInicial("admin", "1234567").run(null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("pelo menos 8");
		verify(usuarioRepository, never()).save(any());
	}
}
