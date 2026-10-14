package br.com.controlecanhotos.seguranca;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import br.com.controlecanhotos.Cte;
import br.com.controlecanhotos.TestcontainersConfiguration;
import br.com.controlecanhotos.usuario.Perfil;
import br.com.controlecanhotos.usuario.Usuario;
import br.com.controlecanhotos.usuario.UsuarioRepository;

/** Com login de verdade: cada registro do histórico guarda quem o fez. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class HistoricoUsuarioTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private UsuarioRepository usuarios;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JdbcTemplate jdbc;

	private String entrar(String login, String senha) throws Exception {
		String resposta = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"login\": \"%s\", \"senha\": \"%s\"}".formatted(login, senha)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return "Bearer " + JsonPath.read(resposta, "$.token");
	}

	/** Cria uma coleta com um CT-e e uma NF de número {@code numero}; devolve o JSON da resposta. */
	private String criarColeta(String token, int numero) throws Exception {
		return mvc.perform(post("/coletas").header(HttpHeaders.AUTHORIZATION, token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"clienteNome": "Cliente H", "clienteCnpj": "55555555000155", "dataProgramada": "2026-10-20",
						 "cargas": [{"chaveAcesso": "%s", "cidadeDestino": "Santos", "ufDestino": "SP",
						 "notasFiscais": [{"numeroNotaFiscal": %d, "pesoKg": 10}]}]}
						""".formatted(Cte.chave(numero), numero)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
	}

	private static long id(String json, String caminho) {
		return ((Number) JsonPath.read(json, caminho)).longValue();
	}

	@Test
	void historicoDaCargaEDoCanhotoGuardaQuemFezCadaMudanca() throws Exception {
		usuarios.save(new Usuario("operador-historico", "Operador", passwordEncoder.encode("senha-forte-1"),
				Perfil.OPERADOR));
		String admin = entrar("admin", "admin12345");
		String operador = entrar("operador-historico", "senha-forte-1");

		String coleta = criarColeta(admin, 4001);
		long cargaId = id(coleta, "$.cargas[0].id");
		long notaId = id(coleta, "$.cargas[0].notasFiscais[0].id");

		for (String novoStatus : new String[] { "COLETADA", "EM_TRANSITO", "ENTREGUE" }) {
			mvc.perform(patch("/cargas/{id}/status", cargaId).header(HttpHeaders.AUTHORIZATION, operador)
					.contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"%s\"}".formatted(novoStatus)))
					.andExpect(status().isOk());
		}
		mvc.perform(post("/notas-fiscais/{id}/canhoto/registros", notaId).header(HttpHeaders.AUTHORIZATION, operador)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\": \"RECEBIDO\", \"dataRecebimento\": \"%s\"}"
						.formatted(LocalDate.now(ZoneId.of("America/Sao_Paulo")))))
				.andExpect(status().isOk());

		mvc.perform(get("/cargas/{id}", cargaId).header(HttpHeaders.AUTHORIZATION, admin))
				.andExpect(jsonPath("$.historico.length()").value(4))
				.andExpect(jsonPath("$.historico[0].alteradoPor").value("admin"))
				.andExpect(jsonPath("$.historico[1].alteradoPor").value("operador-historico"))
				.andExpect(jsonPath("$.historico[3].alteradoPor").value("operador-historico"));
		mvc.perform(get("/notas-fiscais/{id}/canhoto", notaId).header(HttpHeaders.AUTHORIZATION, admin))
				.andExpect(jsonPath("$.historico[0].registradoPor").value("admin"))
				.andExpect(jsonPath("$.historico[1].registradoPor").value("operador-historico"));
	}

	@Test
	void historicoSoAceitaLoginDeUsuarioCadastrado() throws Exception {
		long cargaId = id(criarColeta(entrar("admin", "admin12345"), 4002), "$.cargas[0].id");

		assertThatThrownBy(() -> jdbc.update(
				"insert into historico_status (carga_id, status_novo, alterado_por) values (?, 'COLETADA', ?)",
				cargaId, "ninguem"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}
}
