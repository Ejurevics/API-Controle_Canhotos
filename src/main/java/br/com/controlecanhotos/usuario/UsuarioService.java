package br.com.controlecanhotos.usuario;

import java.nio.charset.StandardCharsets;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PagedModel;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.controlecanhotos.shared.exception.RecursoNaoEncontradoException;
import br.com.controlecanhotos.shared.exception.RegraNegocioException;
import br.com.controlecanhotos.shared.paginacao.ParametrosListagem;
import br.com.controlecanhotos.usuario.dto.CriarUsuarioRequest;
import br.com.controlecanhotos.usuario.dto.UsuarioResponse;

@Service
public class UsuarioService {

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;

	public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public UsuarioResponse criar(CriarUsuarioRequest request) {
		String login = Usuario.normalizarLogin(request.login());
		if (usuarioRepository.existsByLogin(login)) {
			throw new RegraNegocioException("Login já cadastrado: " + login);
		}
		Usuario usuario = new Usuario(login, request.nome().strip(), codificar(request.senha()), request.perfil());
		return UsuarioResponse.de(usuarioRepository.save(usuario));
	}

	@Transactional(readOnly = true)
	public UsuarioResponse buscar(Long id) {
		return UsuarioResponse.de(encontrar(id));
	}

	@Transactional(readOnly = true)
	public PagedModel<UsuarioResponse> listar(Perfil perfil, Boolean ativo, int pagina, int tamanho) {
		Specification<Usuario> filtro = (root, query, cb) -> cb.conjunction();
		if (perfil != null) {
			filtro = filtro.and((root, query, cb) -> cb.equal(root.get("perfil"), perfil));
		}
		if (ativo != null) {
			filtro = filtro.and((root, query, cb) -> cb.equal(root.get("ativo"), ativo));
		}

		Sort ordenacao = Sort.by(Sort.Order.asc("login"));
		return new PagedModel<>(usuarioRepository
				.findAll(filtro, ParametrosListagem.paginar(pagina, tamanho, ordenacao))
				.map(UsuarioResponse::de));
	}

	/**
	 * Ninguém desativa a si mesmo, e o último ADMIN ativo não pode ser desativado: sem ele, ninguém mais
	 * gerencia usuários (o ADMIN inicial só é criado quando não existe ADMIN nenhum, nem inativo).
	 */
	@Transactional
	public UsuarioResponse alterarAtivo(Long id, boolean ativo, String loginDoSolicitante) {
		Usuario usuario = encontrar(id);
		if (!ativo && usuario.isAtivo()) {
			if (usuario.getLogin().equals(Usuario.normalizarLogin(loginDoSolicitante))) {
				throw new RegraNegocioException("Não é possível desativar o próprio usuário");
			}
			if (usuario.getPerfil() == Perfil.ADMIN && usuarioRepository.countByPerfilAndAtivoTrue(Perfil.ADMIN) <= 1) {
				throw new RegraNegocioException("Não é possível desativar o último ADMIN ativo");
			}
		}
		if (ativo) {
			usuario.ativar();
		} else {
			usuario.desativar();
		}
		return UsuarioResponse.de(usuario);
	}

	/** ADMIN define uma nova senha para outro usuário (por exemplo, quando ele a esqueceu). */
	@Transactional
	public void redefinirSenha(Long id, String novaSenha) {
		encontrar(id).alterarSenha(codificar(novaSenha));
	}

	/** O próprio usuário troca a senha, confirmando a atual. */
	@Transactional
	public void trocarPropriaSenha(String login, String senhaAtual, String novaSenha) {
		Usuario usuario = usuarioRepository.findByLogin(Usuario.normalizarLogin(login))
				.filter(Usuario::isAtivo)
				.orElseThrow(() -> new InsufficientAuthenticationException("Usuário do token não está ativo"));
		if (!passwordEncoder.matches(senhaAtual, usuario.getSenhaHash())) {
			throw new RegraNegocioException("Senha atual incorreta");
		}
		usuario.alterarSenha(codificar(novaSenha));
	}

	private Usuario encontrar(Long id) {
		return usuarioRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + id));
	}

	/** O tamanho em caracteres já foi validado no DTO; aqui vale o limite do BCrypt, que é em bytes. */
	private String codificar(String senha) {
		if (senha.getBytes(StandardCharsets.UTF_8).length > Usuario.TAMANHO_MAXIMO_SENHA) {
			throw new RegraNegocioException(
					"Senha deve ter no máximo " + Usuario.TAMANHO_MAXIMO_SENHA + " bytes em UTF-8");
		}
		return passwordEncoder.encode(senha);
	}
}
