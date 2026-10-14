package br.com.controlecanhotos.seguranca;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.controlecanhotos.seguranca.dto.LoginResponse;
import br.com.controlecanhotos.usuario.Usuario;
import br.com.controlecanhotos.usuario.UsuarioRepository;

@Service
public class AutenticacaoService {

	static final String CREDENCIAIS_INVALIDAS = "Login ou senha inválidos";

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;
	private final TokenService tokenService;

	/** Usado quando o login não existe, para a resposta levar o mesmo tempo que uma senha errada. */
	private final String hashFicticio;

	public AutenticacaoService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
			TokenService tokenService) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
		this.hashFicticio = passwordEncoder.encode("senha-ficticia-para-login-inexistente");
	}

	/** Login inexistente, inativo ou senha errada dão a mesma resposta, sem dizer qual foi. */
	@Transactional(readOnly = true)
	public LoginResponse entrar(String login, String senha) {
		Usuario usuario = usuarioRepository.findByLogin(Usuario.normalizarLogin(login)).orElse(null);
		if (usuario == null) {
			passwordEncoder.matches(senha, hashFicticio);
			throw new BadCredentialsException(CREDENCIAIS_INVALIDAS);
		}
		if (!passwordEncoder.matches(senha, usuario.getSenhaHash()) || !usuario.isAtivo()) {
			throw new BadCredentialsException(CREDENCIAIS_INVALIDAS);
		}
		TokenService.Token token = tokenService.gerar(usuario);
		return new LoginResponse(token.valor(), "Bearer", token.expiraEm(), usuario.getPerfil());
	}
}
