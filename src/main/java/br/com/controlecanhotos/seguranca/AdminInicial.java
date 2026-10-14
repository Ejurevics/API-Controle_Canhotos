package br.com.controlecanhotos.seguranca;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import br.com.controlecanhotos.usuario.Perfil;
import br.com.controlecanhotos.usuario.Usuario;
import br.com.controlecanhotos.usuario.UsuarioRepository;

/**
 * Na subida, se ainda não existe nenhum ADMIN, cria um a partir de {@code app.seguranca.admin-inicial}
 * (ADMIN_LOGIN, ADMIN_NOME, ADMIN_SENHA). Depois disso, a configuração é ignorada.
 */
@Component
public class AdminInicial implements ApplicationRunner {

	static final int TAMANHO_MINIMO_SENHA = Usuario.TAMANHO_MINIMO_SENHA;

	private static final Logger LOG = LoggerFactory.getLogger(AdminInicial.class);

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;
	private final PropriedadesSeguranca propriedades;

	public AdminInicial(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
			PropriedadesSeguranca propriedades) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
		this.propriedades = propriedades;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (usuarioRepository.existsByPerfil(Perfil.ADMIN)) {
			return;
		}
		PropriedadesSeguranca.AdminInicial admin = propriedades.adminInicial();
		if (admin == null || isBlank(admin.login()) || isBlank(admin.senha())) {
			LOG.warn("Nenhum ADMIN cadastrado e ADMIN_LOGIN/ADMIN_SENHA não configurados: ninguém consegue entrar");
			return;
		}
		if (admin.senha().length() < TAMANHO_MINIMO_SENHA) {
			throw new IllegalStateException(
					"ADMIN_SENHA deve ter pelo menos " + TAMANHO_MINIMO_SENHA + " caracteres");
		}
		String nome = isBlank(admin.nome()) ? "Administrador" : admin.nome();
		usuarioRepository.save(
				new Usuario(admin.login(), nome, passwordEncoder.encode(admin.senha()), Perfil.ADMIN));
		LOG.info("ADMIN inicial criado: {}", Usuario.normalizarLogin(admin.login()));
	}

	private static boolean isBlank(String valor) {
		return valor == null || valor.isBlank();
	}
}
