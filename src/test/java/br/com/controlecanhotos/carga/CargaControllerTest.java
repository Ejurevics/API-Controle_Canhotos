package br.com.controlecanhotos.carga;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import br.com.controlecanhotos.Cte;
import br.com.controlecanhotos.TestcontainersConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CargaControllerTest {

	private static final AtomicInteger NUMERO_NF = new AtomicInteger(2000);

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	private long criarCarga() throws Exception {
		int numeroNf = NUMERO_NF.incrementAndGet();
		String body = """
				{"clienteNome": "Cliente B", "clienteCnpj": "33333333000133", "dataProgramada": "2026-10-20",
				 "cargas": [{"chaveAcesso": "%s", "cidadeDestino": "Santos", "ufDestino": "SP",
				 "notasFiscais": [{"numeroNotaFiscal": %d, "pesoKg": 10}]}]}
				""".formatted(Cte.chave(numeroNf), numeroNf);
		String resposta = mvc.perform(post("/coletas").contentType(MediaType.APPLICATION_JSON).content(body))
				.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(resposta, "$.cargas[0].id")).longValue();
	}

	private String corpo(String status, String motivo) {
		String motivoJson = motivo == null ? "null" : "\"" + motivo + "\"";
		return "{\"status\": \"%s\", \"motivo\": %s}".formatted(status, motivoJson);
	}

	@Test
	void alteraStatusERegistraHistorico() throws Exception {
		long id = criarCarga();

		mvc.perform(patch("/cargas/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
				.content(corpo("COLETADA", null)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("COLETADA"))
				.andExpect(jsonPath("$.historico.length()").value(2))
				.andExpect(jsonPath("$.historico[1].statusAnterior").value("AGUARDANDO_COLETA"))
				.andExpect(jsonPath("$.historico[1].statusNovo").value("COLETADA"));

		mvc.perform(get("/cargas/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("COLETADA"));
	}

	@Test
	void rejeitaTransicaoInvalida() throws Exception {
		long id = criarCarga();

		mvc.perform(patch("/cargas/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
				.content(corpo("ENTREGUE", null)))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value(containsString("Transição de status inválida")));
	}

	@Test
	void ocorrenciaExigeMotivo() throws Exception {
		long id = criarCarga();

		mvc.perform(patch("/cargas/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
				.content(corpo("OCORRENCIA", null)))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value(containsString("Motivo")));

		mvc.perform(patch("/cargas/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
				.content(corpo("OCORRENCIA", "Endereço fechado")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.historico[1].motivo").value("Endereço fechado"));
	}

	@Test
	void rejeitaStatusAusenteOuDesconhecido() throws Exception {
		long id = criarCarga();

		mvc.perform(patch("/cargas/{id}/status", id).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros.status").exists());

		mvc.perform(patch("/cargas/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
				.content(corpo("VOANDO", null)))
				.andExpect(status().isBadRequest());
	}

	/** Deixa a carga como uma migrada pela V3: chave provisória com prefixo 99. */
	private void tornarProvisoria(long id) {
		jdbc.update("UPDATE carga SET chave_acesso = ?, chave_provisoria = TRUE WHERE id = ?",
				"99" + "%042d".formatted(id), id);
	}

	private static String chave(String chave) {
		return "{\"chaveAcesso\": \"%s\"}".formatted(chave);
	}

	@Test
	void listaECorrigeChaveProvisoria() throws Exception {
		long id = criarCarga();
		tornarProvisoria(id);

		mvc.perform(get("/cargas").param("chaveProvisoria", "true").param("tamanho", "100"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].id").value(org.hamcrest.Matchers.hasItem((int) id)));

		mvc.perform(patch("/cargas/{id}/chave-acesso", id).contentType(MediaType.APPLICATION_JSON)
				.content(chave(Cte.chave(2901))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.chaveAcesso").value(Cte.chave(2901)))
				.andExpect(jsonPath("$.chaveProvisoria").value(false));

		mvc.perform(get("/cargas").param("chaveProvisoria", "true").param("tamanho", "100"))
				.andExpect(jsonPath("$.content[*].id").value(
						org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem((int) id))));

		mvc.perform(patch("/cargas/{id}/chave-acesso", id).contentType(MediaType.APPLICATION_JSON)
				.content(chave(Cte.chave(2902))))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value(containsString("já é definitiva")));
	}

	@Test
	void naoCorrigeParaChaveJaUsadaNemInvalida() throws Exception {
		long outra = criarCarga();
		String chaveEmUso = ((String) JsonPath.read(
				mvc.perform(get("/cargas/{id}", outra)).andReturn().getResponse().getContentAsString(),
				"$.chaveAcesso"));
		long id = criarCarga();
		tornarProvisoria(id);

		mvc.perform(patch("/cargas/{id}/chave-acesso", id).contentType(MediaType.APPLICATION_JSON)
				.content(chave(chaveEmUso)))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value(containsString("CT-e já cadastrado")));

		String digitoErrado = Cte.chave(2903).substring(0, 43)
				+ (Cte.chave(2903).charAt(43) - '0' + 1) % 10;
		mvc.perform(patch("/cargas/{id}/chave-acesso", id).contentType(MediaType.APPLICATION_JSON)
				.content(chave(digitoErrado)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros.chaveAcesso").value(containsString("dígito verificador")));

		mvc.perform(get("/cargas/{id}", id)).andExpect(jsonPath("$.chaveProvisoria").value(true));
	}

	@Test
	void retorna404ParaCargaInexistente() throws Exception {
		mvc.perform(get("/cargas/999999")).andExpect(status().isNotFound());
		mvc.perform(patch("/cargas/999999/status").contentType(MediaType.APPLICATION_JSON)
				.content(corpo("COLETADA", null)))
				.andExpect(status().isNotFound());
	}
}
