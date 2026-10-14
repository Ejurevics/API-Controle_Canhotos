package br.com.controlecanhotos.canhoto;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
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
class CanhotoControllerTest {

	private static final AtomicInteger NUMERO_NF = new AtomicInteger(3000);

	@Autowired
	private MockMvc mvc;

	private long criarNota(String... statusSequencia) throws Exception {
		int numeroNf = NUMERO_NF.incrementAndGet();
		String body = """
				{"clienteNome": "Cliente C", "clienteCnpj": "44444444000144", "dataProgramada": "2026-10-20",
				 "cargas": [{"chaveAcesso": "%s", "cidadeDestino": "Santos", "ufDestino": "SP",
				 "notasFiscais": [{"numeroNotaFiscal": %d, "pesoKg": 10}]}]}
				""".formatted(Cte.chave(numeroNf), numeroNf);
		String resposta = mvc.perform(post("/coletas").contentType(MediaType.APPLICATION_JSON).content(body))
				.andReturn().getResponse().getContentAsString();
		long cargaId = ((Number) JsonPath.read(resposta, "$.cargas[0].id")).longValue();
		for (String novoStatus : statusSequencia) {
			mvc.perform(patch("/cargas/{id}/status", cargaId).contentType(MediaType.APPLICATION_JSON)
					.content("{\"status\": \"%s\"}".formatted(novoStatus))).andExpect(status().isOk());
		}
		return ((Number) JsonPath.read(resposta, "$.cargas[0].notasFiscais[0].id")).longValue();
	}

	private long criarNotaEntregue() throws Exception {
		return criarNota("COLETADA", "EM_TRANSITO", "ENTREGUE");
	}

	/** Mesmo fuso que a API usa para decidir se a data é futura (o CI roda em UTC). */
	private static LocalDate hoje() {
		return LocalDate.now(ZoneId.of("America/Sao_Paulo"));
	}

	private String corpo(String status, LocalDate data, String observacao, Boolean confirmada) {
		return """
				{"status": "%s", "dataRecebimento": "%s", "observacao": %s, "dataFuturaConfirmada": %s}
				""".formatted(status, data, observacao == null ? "null" : "\"" + observacao + "\"", confirmada);
	}

	@Test
	void registraRecebimento() throws Exception {
		long id = criarNotaEntregue();

		mvc.perform(post("/notas-fiscais/{id}/canhoto/registros", id).contentType(MediaType.APPLICATION_JSON)
				.content(corpo("RECEBIDO", hoje(), null, null)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("RECEBIDO"))
				.andExpect(jsonPath("$.dataRecebimento").value(hoje().toString()))
				.andExpect(jsonPath("$.historico.length()").value(2));

		mvc.perform(get("/notas-fiscais/{id}/canhoto", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("RECEBIDO"));
	}

	@Test
	void divergenciaDepoisRecebimentoPreservaHistorico() throws Exception {
		long id = criarNotaEntregue();

		mvc.perform(post("/notas-fiscais/{id}/canhoto/registros", id).contentType(MediaType.APPLICATION_JSON)
				.content(corpo("DIVERGENTE", hoje(), "Assinatura ilegível", false)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DIVERGENTE"))
				.andExpect(jsonPath("$.observacao").value("Assinatura ilegível"));

		mvc.perform(post("/notas-fiscais/{id}/canhoto/registros", id).contentType(MediaType.APPLICATION_JSON)
				.content(corpo("RECEBIDO", hoje(), null, false)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("RECEBIDO"))
				.andExpect(jsonPath("$.observacao").doesNotExist())
				.andExpect(jsonPath("$.historico.length()").value(3))
				.andExpect(jsonPath("$.historico[1].observacao").value("Assinatura ilegível"));
	}

	@Test
	void divergenciaExigeObservacao() throws Exception {
		long id = criarNotaEntregue();

		mvc.perform(post("/notas-fiscais/{id}/canhoto/registros", id).contentType(MediaType.APPLICATION_JSON)
				.content(corpo("DIVERGENTE", hoje(), null, false)))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value(containsString("Observação")));
	}

	@Test
	void exigeCargaEntregue() throws Exception {
		long id = criarNota("COLETADA");

		mvc.perform(post("/notas-fiscais/{id}/canhoto/registros", id).contentType(MediaType.APPLICATION_JSON)
				.content(corpo("RECEBIDO", hoje(), null, false)))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value(containsString("ENTREGUE")));
	}

	@Test
	void dataFuturaPedeConfirmacaoEPassaQuandoConfirmada() throws Exception {
		long id = criarNotaEntregue();
		LocalDate futura = hoje().plusDays(30);

		mvc.perform(post("/notas-fiscais/{id}/canhoto/registros", id).contentType(MediaType.APPLICATION_JSON)
				.content(corpo("RECEBIDO", futura, null, false)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.confirmacaoNecessaria").value(true));

		mvc.perform(get("/notas-fiscais/{id}/canhoto", id))
				.andExpect(jsonPath("$.status").value("PENDENTE"));

		mvc.perform(post("/notas-fiscais/{id}/canhoto/registros", id).contentType(MediaType.APPLICATION_JSON)
				.content(corpo("RECEBIDO", futura, null, true)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("RECEBIDO"))
				.andExpect(jsonPath("$.historico[1].dataFuturaConfirmada").value(true));
	}

	@Test
	void naoPermiteRegistrarComoPendente() throws Exception {
		long id = criarNotaEntregue();

		mvc.perform(post("/notas-fiscais/{id}/canhoto/registros", id).contentType(MediaType.APPLICATION_JSON)
				.content(corpo("PENDENTE", hoje(), null, false)))
				.andExpect(status().isUnprocessableContent());
	}

	@Test
	void validaCamposObrigatorios() throws Exception {
		long id = criarNotaEntregue();

		mvc.perform(post("/notas-fiscais/{id}/canhoto/registros", id).contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros.status").exists())
				.andExpect(jsonPath("$.erros.dataRecebimento").exists());
	}

	@Test
	void retorna404ParaCargaInexistente() throws Exception {
		mvc.perform(get("/notas-fiscais/999999/canhoto")).andExpect(status().isNotFound());
		mvc.perform(post("/notas-fiscais/999999/canhoto/registros").contentType(MediaType.APPLICATION_JSON)
				.content(corpo("RECEBIDO", hoje(), null, false)))
				.andExpect(status().isNotFound());
	}
}
