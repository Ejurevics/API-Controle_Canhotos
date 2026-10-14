package br.com.controlecanhotos.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import br.com.controlecanhotos.TestcontainersConfiguration;
import br.com.controlecanhotos.usuario.Perfil;
import br.com.controlecanhotos.usuario.Usuario;
import br.com.controlecanhotos.usuario.UsuarioRepository;

/**
 * Autenticação de verdade: sem o atalho de {@code AutenticadoComoAdmin}. O ADMIN inicial vem dos valores do
 * perfil dev (admin / admin12345), como numa subida local.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SegurancaTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private UsuarioRepository usuarios;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtDecoder jwtDecoder;

	@Autowired
	private JwtEncoder jwtEncoder;

	private static String credenciais(String login, String senha) {
		return "{\"login\": \"%s\", \"senha\": \"%s\"}".formatted(login, senha);
	}

	private String entrar(String login, String senha) throws Exception {
		String resposta = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(credenciais(login, senha)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(resposta, "$.token");
	}

	private static String bearer(String token) {
		return "Bearer " + token;
	}

	private void criarUsuario(String login, Perfil perfil, boolean ativo) {
		Usuario usuario = new Usuario(login, "Usuário " + login, passwordEncoder.encode("senha-forte-1"), perfil);
		if (!ativo) {
			usuario.desativar();
		}
		usuarios.save(usuario);
	}

	@Test
	void semTokenResponde401EmProblemDetail() throws Exception {
		mvc.perform(get("/coletas"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.detail").value(containsString("Autenticação necessária")));
	}

	@Test
	void healthEDocumentacaoSaoPublicos() throws Exception {
		mvc.perform(get("/actuator/health")).andExpect(status().isOk());
		mvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.components.securitySchemes.bearer.scheme").value("bearer"));
	}

	@Test
	void adminInicialEntraEUsaOToken() throws Exception {
		String resposta = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(credenciais("admin", "admin12345")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tipo").value("Bearer"))
				.andExpect(jsonPath("$.perfil").value("ADMIN"))
				.andExpect(jsonPath("$.expiraEm").exists())
				.andReturn().getResponse().getContentAsString();
		String token = JsonPath.read(resposta, "$.token");

		Jwt jwt = jwtDecoder.decode(token);
		assertThat(jwt.getSubject()).isEqualTo("admin");
		assertThat(jwt.getClaimAsString("perfil")).isEqualTo("ADMIN");
		assertThat(jwt.getClaimAsString("iss")).isEqualTo(SegurancaConfig.EMISSOR);
		assertThat(jwt.getExpiresAt()).isCloseTo(Instant.now().plus(1, ChronoUnit.HOURS),
				org.assertj.core.api.Assertions.within(1, ChronoUnit.MINUTES));

		mvc.perform(get("/coletas").header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isOk());
	}

	@Test
	void loginIgnoraMaiusculasEEspacosNasPontas() throws Exception {
		entrar("  ADMIN ", "admin12345");
	}

	@Test
	void senhaErradaELoginInexistenteDaoAMesmaResposta() throws Exception {
		for (String[] tentativa : new String[][] { { "admin", "senha-errada" }, { "ninguem", "admin12345" } }) {
			mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
					.content(credenciais(tentativa[0], tentativa[1])))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.detail").value("Login ou senha inválidos"));
		}
	}

	@Test
	void validaCamposDoLogin() throws Exception {
		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros.login").exists())
				.andExpect(jsonPath("$.erros.senha").exists());
	}

	@Test
	void usuarioInativoNaoEntra() throws Exception {
		criarUsuario("inativo", Perfil.OPERADOR, false);

		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(credenciais("inativo", "senha-forte-1")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("Login ou senha inválidos"));
	}

	@Test
	void operadorFazODiaADiaMasNaoCorrigeChaveDeAcesso() throws Exception {
		criarUsuario("operador", Perfil.OPERADOR, true);
		String token = entrar("operador", "senha-forte-1");

		mvc.perform(get("/coletas").header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isOk());
		mvc.perform(patch("/cargas/1/chave-acesso").header(HttpHeaders.AUTHORIZATION, bearer(token))
				.contentType(MediaType.APPLICATION_JSON).content("{\"chaveAcesso\": \"x\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.detail").value(containsString("não tem permissão")));
	}

	/**
	 * Tentativa de login vinda de {@code ip}. Os testes do limite usam IPs próprios, porque o contador fica no
	 * contexto Spring compartilhado entre as classes de teste.
	 */
	private ResultActions tentar(String login, String senha, String ip) throws Exception {
		return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(credenciais(login, senha))
				.with(requisicao -> {
					requisicao.setRemoteAddr(ip);
					return requisicao;
				}));
	}

	@Test
	void bloqueiaOLoginDepoisDeCincoFalhasMesmoComASenhaCerta() throws Exception {
		criarUsuario("alvo-forca-bruta", Perfil.OPERADOR, true);
		for (int i = 0; i < 5; i++) {
			tentar("alvo-forca-bruta", "senha-errada", "10.1.0.1").andExpect(status().isUnauthorized());
		}

		tentar("alvo-forca-bruta", "senha-forte-1", "10.1.0.2")
				.andExpect(status().isTooManyRequests())
				// Até 900 s (15 min), descontado o tempo das tentativas anteriores.
				.andExpect(header().string(HttpHeaders.RETRY_AFTER, matchesPattern("8[6-9]\\d|900")))
				.andExpect(jsonPath("$.status").value(429))
				.andExpect(jsonPath("$.detail").value(containsString("Muitas tentativas de login")));
	}

	@Test
	void loginInexistenteTambemEhBloqueado() throws Exception {
		for (int i = 0; i < 5; i++) {
			tentar("ninguem-aqui", "qualquer", "10.2.0.1").andExpect(status().isUnauthorized());
		}

		tentar("ninguem-aqui", "qualquer", "10.2.0.1").andExpect(status().isTooManyRequests());
	}

	@Test
	void bloqueiaOIpQueErraEmVariosLogins() throws Exception {
		for (int i = 0; i < 20; i++) {
			tentar("spray-" + i, "senha-comum", "10.3.0.1").andExpect(status().isUnauthorized());
		}

		tentar("admin", "admin12345", "10.3.0.1").andExpect(status().isTooManyRequests());
		tentar("admin", "admin12345", "10.3.0.2").andExpect(status().isOk());
	}

	@Test
	void loginCertoZeraAsFalhasDoLogin() throws Exception {
		criarUsuario("erra-e-acerta", Perfil.OPERADOR, true);
		for (int i = 0; i < 4; i++) {
			tentar("erra-e-acerta", "senha-errada", "10.4.0.1").andExpect(status().isUnauthorized());
		}
		tentar("erra-e-acerta", "senha-forte-1", "10.4.0.1").andExpect(status().isOk());

		for (int i = 0; i < 4; i++) {
			tentar("erra-e-acerta", "senha-errada", "10.4.0.1").andExpect(status().isUnauthorized());
		}
		tentar("erra-e-acerta", "senha-forte-1", "10.4.0.1").andExpect(status().isOk());
	}

	@Test
	void operadorNaoGerenciaUsuarios() throws Exception {
		criarUsuario("operador-sem-gestao", Perfil.OPERADOR, true);
		String token = entrar("operador-sem-gestao", "senha-forte-1");

		mvc.perform(get("/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isForbidden());
		mvc.perform(post("/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(token))
				.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void usuarioTrocaAPropriaSenhaEEntraComANova() throws Exception {
		criarUsuario("troca-senha", Perfil.OPERADOR, true);
		String token = entrar("troca-senha", "senha-forte-1");

		mvc.perform(patch("/auth/senha").header(HttpHeaders.AUTHORIZATION, bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"senhaAtual\": \"errada\", \"novaSenha\": \"senha-nova-12\"}"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value("Senha atual incorreta"));
		mvc.perform(patch("/auth/senha").header(HttpHeaders.AUTHORIZATION, bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"senhaAtual\": \"senha-forte-1\", \"novaSenha\": \"senha-nova-12\"}"))
				.andExpect(status().isNoContent());

		entrar("troca-senha", "senha-nova-12");
	}

	@Test
	void trocarSenhaExigeToken() throws Exception {
		mvc.perform(patch("/auth/senha").contentType(MediaType.APPLICATION_JSON)
				.content("{\"senhaAtual\": \"admin12345\", \"novaSenha\": \"outra-senha-1\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void adminNaoDesativaASiMesmo() throws Exception {
		String token = entrar("admin", "admin12345");
		long id = usuarios.findByLogin("admin").orElseThrow().getId();

		mvc.perform(patch("/usuarios/{id}/ativo", id).header(HttpHeaders.AUTHORIZATION, bearer(token))
				.contentType(MediaType.APPLICATION_JSON).content("{\"ativo\": false}"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value("Não é possível desativar o próprio usuário"));
	}

	@Test
	void tokenAdulteradoExpiradoOuDeOutroEmissorResponde401() throws Exception {
		String valido = entrar("admin", "admin12345");
		String adulterado = valido.substring(0, valido.length() - 2) + (valido.endsWith("A") ? "BB" : "AA");
		String expirado = token(SegurancaConfig.EMISSOR, Instant.now().minus(2, ChronoUnit.HOURS));
		String outroEmissor = token("outro-sistema", Instant.now());

		for (String token : new String[] { adulterado, expirado, outroEmissor }) {
			mvc.perform(get("/coletas").header(HttpHeaders.AUTHORIZATION, bearer(token)))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.status").value(401));
		}
	}

	/** Token assinado com a chave certa, mas com emissor e data de emissão escolhidos (vale 1h). */
	private String token(String emissor, Instant emitidoEm) {
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(emissor)
				.subject("admin")
				.issuedAt(emitidoEm)
				.expiresAt(emitidoEm.plus(1, ChronoUnit.HOURS))
				.claim("perfil", "ADMIN")
				.build();
		return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
				.getTokenValue();
	}
}
