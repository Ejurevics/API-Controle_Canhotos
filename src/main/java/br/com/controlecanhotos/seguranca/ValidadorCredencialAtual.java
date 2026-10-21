package br.com.controlecanhotos.seguranca;

import java.util.Optional;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import br.com.controlecanhotos.usuario.UsuarioRepository;

/**
 * Aceita o token só se o usuário ainda existe, está ativo e a versão da credencial no token é a atual (ADR 0004).
 * Desativar o usuário ou trocar a senha muda a versão e derruba os tokens emitidos antes. A credencial vem do
 * {@link CacheCredenciais} (ADR 0005), para não consultar o banco a cada requisição.
 */
@Component
public class ValidadorCredencialAtual implements OAuth2TokenValidator<Jwt> {

	private static final OAuth2TokenValidatorResult REVOGADO = OAuth2TokenValidatorResult.failure(
			new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "Token revogado: entre de novo", null));

	private final UsuarioRepository usuarioRepository;
	private final CacheCredenciais cache;

	public ValidadorCredencialAtual(UsuarioRepository usuarioRepository, CacheCredenciais cache) {
		this.usuarioRepository = usuarioRepository;
		this.cache = cache;
	}

	@Override
	public OAuth2TokenValidatorResult validate(Jwt jwt) {
		Object versao = jwt.getClaim(SegurancaConfig.CLAIM_VERSAO_CREDENCIAL);
		if (!(versao instanceof Number numero)) {
			return REVOGADO;
		}
		boolean valido = cache.obter(jwt.getSubject(), this::carregar)
				.filter(CacheCredenciais.Credencial::ativo)
				.filter(credencial -> credencial.versao() == numero.longValue())
				.isPresent();
		return valido ? OAuth2TokenValidatorResult.success() : REVOGADO;
	}

	private Optional<CacheCredenciais.Credencial> carregar(String login) {
		return usuarioRepository.findByLogin(login)
				.map(usuario -> new CacheCredenciais.Credencial(usuario.isAtivo(), usuario.getVersaoCredencial()));
	}
}
