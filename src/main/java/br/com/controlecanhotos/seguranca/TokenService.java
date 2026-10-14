package br.com.controlecanhotos.seguranca;

import java.time.Clock;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import br.com.controlecanhotos.usuario.Usuario;

@Service
public class TokenService {

	public record Token(String valor, Instant expiraEm) {
	}

	private final JwtEncoder encoder;
	private final PropriedadesSeguranca propriedades;
	private final Clock relogio;

	@Autowired
	public TokenService(JwtEncoder encoder, PropriedadesSeguranca propriedades) {
		this(encoder, propriedades, Clock.systemUTC());
	}

	TokenService(JwtEncoder encoder, PropriedadesSeguranca propriedades, Clock relogio) {
		this.encoder = encoder;
		this.propriedades = propriedades;
		this.relogio = relogio;
	}

	public Token gerar(Usuario usuario) {
		Instant agora = relogio.instant();
		Instant expiraEm = agora.plus(propriedades.jwt().expiracao());
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(SegurancaConfig.EMISSOR)
				.subject(usuario.getLogin())
				.issuedAt(agora)
				.expiresAt(expiraEm)
				.claim(SegurancaConfig.CLAIM_PERFIL, usuario.getPerfil().name())
				.claim("nome", usuario.getNome())
				.build();
		JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
		String valor = encoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
		return new Token(valor, expiraEm);
	}
}
