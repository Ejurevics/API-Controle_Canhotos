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

	@Test
	void retorna404ParaCargaInexistente() throws Exception {
		mvc.perform(get("/cargas/999999")).andExpect(status().isNotFound());
		mvc.perform(patch("/cargas/999999/status").contentType(MediaType.APPLICATION_JSON)
				.content(corpo("COLETADA", null)))
				.andExpect(status().isNotFound());
	}
}
