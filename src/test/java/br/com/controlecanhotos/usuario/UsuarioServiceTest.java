package br.com.controlecanhotos.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.controlecanhotos.shared.exception.RecursoNaoEncontradoException;
import br.com.controlecanhotos.shared.exception.RegraNegocioException;
import br.com.controlecanhotos.usuario.dto.CriarUsuarioRequest;
import br.com.controlecanhotos.usuario.dto.UsuarioResponse;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	private UsuarioService service;

	@BeforeEach
	void criarService() {
		service = new UsuarioService(usuarioRepository, passwordEncoder);
	}

	private static Usuario usuario(String login, Perfil perfil) {
		return new Usuario(login, "Usuário " + login, "hash-" + login, perfil);
	}

	@Test
	void criaUsuarioComLoginNormalizadoESenhaCodificada() {
		when(usuarioRepository.existsByLogin("maria.silva")).thenReturn(false);
		when(passwordEncoder.encode("senha-forte-1")).thenReturn("hash");
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

		UsuarioResponse resposta = service.criar(
				new CriarUsuarioRequest("  Maria.Silva ", " Maria Silva ", "senha-forte-1", Perfil.OPERADOR));

		assertThat(resposta.login()).isEqualTo("maria.silva");
		assertThat(resposta.nome()).isEqualTo("Maria Silva");
		assertThat(resposta.perfil()).isEqualTo(Perfil.OPERADOR);
		assertThat(resposta.ativo()).isTrue();
	}

	@Test
	void recusaLoginJaCadastrado() {
		when(usuarioRepository.existsByLogin("maria")).thenReturn(true);

		assertThatThrownBy(() -> service.criar(new CriarUsuarioRequest("MARIA", "Maria", "senha-forte-1",
				Perfil.OPERADOR)))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("Login já cadastrado: maria");
		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void recusaSenhaAcimaDe72BytesMesmoComMenosDe72Caracteres() {
		when(usuarioRepository.existsByLogin("maria")).thenReturn(false);
		String senha = "ç".repeat(40); // 40 caracteres, 80 bytes em UTF-8

		assertThatThrownBy(() -> service.criar(new CriarUsuarioRequest("maria", "Maria", senha, Perfil.OPERADOR)))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining("72 bytes");
		verify(passwordEncoder, never()).encode(any());
	}

	@Test
	void buscaUsuarioInexistenteResponde404() {
		when(usuarioRepository.findById(9L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.buscar(9L))
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessage("Usuário não encontrado: 9");
	}

	@Test
	void desativaEReativaUsuario() {
		Usuario operador = usuario("operador", Perfil.OPERADOR);
		when(usuarioRepository.findById(1L)).thenReturn(Optional.of(operador));

		assertThat(service.alterarAtivo(1L, false, "admin").ativo()).isFalse();
		assertThat(operador.getVersaoCredencial()).as("desativar derruba os tokens").isEqualTo(1);
		assertThat(service.alterarAtivo(1L, true, "admin").ativo()).isTrue();
		assertThat(operador.getVersaoCredencial()).as("reativar não devolve os tokens").isEqualTo(1);
	}

	@Test
	void naoDesativaOProprioUsuario() {
		Usuario admin = usuario("admin", Perfil.ADMIN);
		when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));

		assertThatThrownBy(() -> service.alterarAtivo(1L, false, " ADMIN "))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("Não é possível desativar o próprio usuário");
		assertThat(admin.isAtivo()).isTrue();
	}

	@Test
	void naoDesativaOUltimoAdminAtivo() {
		Usuario admin = usuario("outro-admin", Perfil.ADMIN);
		when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
		when(usuarioRepository.countByPerfilAndAtivoTrue(Perfil.ADMIN)).thenReturn(1L);

		assertThatThrownBy(() -> service.alterarAtivo(1L, false, "admin"))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("Não é possível desativar o último ADMIN ativo");
		assertThat(admin.isAtivo()).isTrue();
	}

	@Test
	void desativaAdminQuandoHaOutroAtivo() {
		Usuario admin = usuario("outro-admin", Perfil.ADMIN);
		when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
		when(usuarioRepository.countByPerfilAndAtivoTrue(Perfil.ADMIN)).thenReturn(2L);

		assertThat(service.alterarAtivo(1L, false, "admin").ativo()).isFalse();
	}

	@Test
	void alteraPerfilEDerrubaOsTokens() {
		Usuario operador = usuario("operador", Perfil.OPERADOR);
		when(usuarioRepository.findById(1L)).thenReturn(Optional.of(operador));

		assertThat(service.alterarPerfil(1L, Perfil.ADMIN, "admin").perfil()).isEqualTo(Perfil.ADMIN);
		assertThat(operador.getVersaoCredencial()).isEqualTo(1);
	}

	@Test
	void mesmoPerfilNaoMudaNada() {
		Usuario admin = usuario("admin", Perfil.ADMIN);
		when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));

		assertThat(service.alterarPerfil(1L, Perfil.ADMIN, "admin").perfil()).isEqualTo(Perfil.ADMIN);
		assertThat(admin.getVersaoCredencial()).isZero();
	}

	@Test
	void naoAlteraOProprioPerfil() {
		Usuario admin = usuario("admin", Perfil.ADMIN);
		when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));

		assertThatThrownBy(() -> service.alterarPerfil(1L, Perfil.OPERADOR, "Admin"))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("Não é possível alterar o próprio perfil");
		assertThat(admin.getPerfil()).isEqualTo(Perfil.ADMIN);
	}

	@Test
	void naoRebaixaOUltimoAdminAtivo() {
		Usuario admin = usuario("outro-admin", Perfil.ADMIN);
		when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
		when(usuarioRepository.countByPerfilAndAtivoTrue(Perfil.ADMIN)).thenReturn(1L);

		assertThatThrownBy(() -> service.alterarPerfil(1L, Perfil.OPERADOR, "admin"))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("Não é possível rebaixar o último ADMIN ativo");
		assertThat(admin.getPerfil()).isEqualTo(Perfil.ADMIN);
	}

	@Test
	void rebaixaAdminInativoSemContarOsAtivos() {
		Usuario admin = usuario("admin-inativo", Perfil.ADMIN);
		admin.desativar();
		when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));

		assertThat(service.alterarPerfil(1L, Perfil.OPERADOR, "admin").perfil()).isEqualTo(Perfil.OPERADOR);
		verify(usuarioRepository, never()).countByPerfilAndAtivoTrue(any());
	}

	@Test
	void adminRedefineSenhaDeOutroUsuario() {
		Usuario operador = usuario("operador", Perfil.OPERADOR);
		when(usuarioRepository.findById(1L)).thenReturn(Optional.of(operador));
		when(passwordEncoder.encode("nova-senha-1")).thenReturn("hash-novo");

		service.redefinirSenha(1L, "nova-senha-1");

		assertThat(operador.getSenhaHash()).isEqualTo("hash-novo");
		assertThat(operador.getVersaoCredencial()).isEqualTo(1);
	}

	@Test
	void usuarioTrocaAPropriaSenhaInformandoAAtual() {
		Usuario operador = usuario("operador", Perfil.OPERADOR);
		when(usuarioRepository.findByLogin("operador")).thenReturn(Optional.of(operador));
		when(passwordEncoder.matches("senha-atual", "hash-operador")).thenReturn(true);
		when(passwordEncoder.encode("nova-senha-1")).thenReturn("hash-novo");

		service.trocarPropriaSenha("operador", "senha-atual", "nova-senha-1");

		assertThat(operador.getSenhaHash()).isEqualTo("hash-novo");
	}

	@Test
	void recusaTrocaComSenhaAtualErrada() {
		Usuario operador = usuario("operador", Perfil.OPERADOR);
		when(usuarioRepository.findByLogin("operador")).thenReturn(Optional.of(operador));
		when(passwordEncoder.matches("errada", "hash-operador")).thenReturn(false);

		assertThatThrownBy(() -> service.trocarPropriaSenha("operador", "errada", "nova-senha-1"))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("Senha atual incorreta");
		assertThat(operador.getSenhaHash()).isEqualTo("hash-operador");
	}

	@Test
	void usuarioDesativadoNaoTrocaSenhaComTokenAntigo() {
		Usuario operador = usuario("operador", Perfil.OPERADOR);
		operador.desativar();
		when(usuarioRepository.findByLogin("operador")).thenReturn(Optional.of(operador));

		assertThatThrownBy(() -> service.trocarPropriaSenha("operador", "senha-atual", "nova-senha-1"))
				.isInstanceOf(InsufficientAuthenticationException.class);
	}
}
