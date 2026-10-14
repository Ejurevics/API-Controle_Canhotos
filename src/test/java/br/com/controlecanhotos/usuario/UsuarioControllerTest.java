package br.com.controlecanhotos.usuario;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import br.com.controlecanhotos.AutenticadoComoAdmin;
import br.com.controlecanhotos.TestcontainersConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, AutenticadoComoAdmin.class })
class UsuarioControllerTest {

	/** O banco é compartilhado entre as classes de teste; cada teste usa logins próprios. */
	private static final AtomicInteger SEQUENCIA = new AtomicInteger();

	@Autowired
	private MockMvc mvc;

	private static String novoLogin() {
		return "usuario-teste-" + SEQUENCIA.incrementAndGet();
	}

	private static String usuario(String login, String perfil) {
		return """
				{"login": "%s", "nome": "Usuário de teste", "senha": "senha-forte-1", "perfil": "%s"}
				""".formatted(login, perfil);
	}

	private long criar(String login, String perfil) throws Exception {
		String resposta = mvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON)
				.content(usuario(login, perfil)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(resposta, "$.id")).longValue();
	}

	@Test
	void criaUsuarioSemExporASenha() throws Exception {
		String login = novoLogin();

		mvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON)
				.content(usuario("  " + login.toUpperCase() + " ", "OPERADOR")))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/usuarios/")))
				.andExpect(jsonPath("$.login").value(login))
				.andExpect(jsonPath("$.perfil").value("OPERADOR"))
				.andExpect(jsonPath("$.ativo").value(true))
				.andExpect(jsonPath("$.criadoEm").exists())
				.andExpect(jsonPath("$.senha").doesNotExist())
				.andExpect(jsonPath("$.senhaHash").doesNotExist());
	}

	@Test
	void validaCamposDoNovoUsuario() throws Exception {
		mvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"login": "a b", "nome": "", "senha": "curta"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros.login").exists())
				.andExpect(jsonPath("$.erros.nome").exists())
				.andExpect(jsonPath("$.erros.senha").exists())
				.andExpect(jsonPath("$.erros.perfil").exists());
	}

	@Test
	void loginRepetidoResponde422() throws Exception {
		String login = novoLogin();
		criar(login, "OPERADOR");

		mvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON)
				.content(usuario(login.toUpperCase(), "ADMIN")))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value("Login já cadastrado: " + login));
	}

	@Test
	void buscaUsuarioPorIdE404QuandoNaoExiste() throws Exception {
		String login = novoLogin();
		long id = criar(login, "ADMIN");

		mvc.perform(get("/usuarios/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.login").value(login))
				.andExpect(jsonPath("$.perfil").value("ADMIN"));
		mvc.perform(get("/usuarios/{id}", 999_999))
				.andExpect(status().isNotFound());
	}

	@Test
	void desativaEFiltraAListagemPorPerfilEAtivo() throws Exception {
		String login = novoLogin();
		long id = criar(login, "OPERADOR");

		mvc.perform(patch("/usuarios/{id}/ativo", id).contentType(MediaType.APPLICATION_JSON)
				.content("{\"ativo\": false}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ativo").value(false));

		mvc.perform(get("/usuarios").param("perfil", "OPERADOR").param("ativo", "false").param("tamanho", "100"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].login").value(hasItem(login)));
		mvc.perform(get("/usuarios").param("ativo", "true").param("tamanho", "100"))
				.andExpect(jsonPath("$.content[*].login").value(not(hasItem(login))));

		mvc.perform(patch("/usuarios/{id}/ativo", id).contentType(MediaType.APPLICATION_JSON)
				.content("{\"ativo\": true}"))
				.andExpect(jsonPath("$.ativo").value(true));
	}

	@Test
	void validaPedidoDeAtivacao() throws Exception {
		long id = criar(novoLogin(), "OPERADOR");

		mvc.perform(patch("/usuarios/{id}/ativo", id).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros.ativo").exists());
	}

	@Test
	void alteraPerfilEValidaOPedido() throws Exception {
		long id = criar(novoLogin(), "OPERADOR");

		mvc.perform(patch("/usuarios/{id}/perfil", id).contentType(MediaType.APPLICATION_JSON)
				.content("{\"perfil\": \"ADMIN\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.perfil").value("ADMIN"));
		mvc.perform(patch("/usuarios/{id}/perfil", id).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros.perfil").exists());
		mvc.perform(patch("/usuarios/{id}/perfil", id).contentType(MediaType.APPLICATION_JSON)
				.content("{\"perfil\": \"CONSULTA\"}"))
				.andExpect(status().isBadRequest());
		mvc.perform(patch("/usuarios/{id}/perfil", 999_999).contentType(MediaType.APPLICATION_JSON)
				.content("{\"perfil\": \"ADMIN\"}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void adminRedefineSenhaEOUsuarioEntraComANova() throws Exception {
		String login = novoLogin();
		long id = criar(login, "OPERADOR");

		mvc.perform(patch("/usuarios/{id}/senha", id).contentType(MediaType.APPLICATION_JSON)
				.content("{\"novaSenha\": \"senha-redefinida-1\"}"))
				.andExpect(status().isNoContent());

		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"login\": \"%s\", \"senha\": \"senha-redefinida-1\"}".formatted(login)))
				.andExpect(status().isOk());
		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"login\": \"%s\", \"senha\": \"senha-forte-1\"}".formatted(login)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void recusaNovaSenhaCurta() throws Exception {
		long id = criar(novoLogin(), "OPERADOR");

		mvc.perform(patch("/usuarios/{id}/senha", id).contentType(MediaType.APPLICATION_JSON)
				.content("{\"novaSenha\": \"curta\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros.novaSenha").exists());
	}
}
