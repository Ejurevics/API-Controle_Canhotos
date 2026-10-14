package br.com.controlecanhotos.coleta;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import br.com.controlecanhotos.TestcontainersConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ColetaControllerTest {

	private static final String CNPJ_CLIENTE = "11111111000111";

	@Autowired
	private MockMvc mvc;

	private static String coleta(String cargas) {
		return """
				{"clienteNome": "Cliente A", "clienteCnpj": "%s", "dataProgramada": "2026-10-20",
				 "cargas": [%s]}
				""".formatted(CNPJ_CLIENTE, cargas);
	}

	private static String carga(int numero, String extras) {
		return """
				{"numeroNotaFiscal": %d, "pesoKg": 150.5, "cidadeDestino": "Campinas",
				 "ufDestino": "SP"%s}
				""".formatted(numero, extras);
	}

	@Test
	void criaColetaComCargasAplicandoPadroes() throws Exception {
		mvc.perform(post("/coletas").contentType(MediaType.APPLICATION_JSON)
				.content(coleta(carga(1001, ""))))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/coletas/")))
				.andExpect(jsonPath("$.situacao").value("PROGRAMADA"))
				.andExpect(jsonPath("$.cargas[0].emitenteCnpj").value(CNPJ_CLIENTE))
				.andExpect(jsonPath("$.cargas[0].serieNotaFiscal").value(1))
				.andExpect(jsonPath("$.cargas[0].status").value("AGUARDANDO_COLETA"))
				.andExpect(jsonPath("$.cargas[0].statusCanhoto").value("PENDENTE"));
	}

	@Test
	void respeitaEmitenteESerieInformados() throws Exception {
		mvc.perform(post("/coletas").contentType(MediaType.APPLICATION_JSON)
				.content(coleta(carga(1002, ", \"emitenteCnpj\": \"22222222000122\", \"serieNotaFiscal\": 3"))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.cargas[0].emitenteCnpj").value("22222222000122"))
				.andExpect(jsonPath("$.cargas[0].serieNotaFiscal").value(3));
	}

	@Test
	void buscaColetaCriada() throws Exception {
		String location = mvc.perform(post("/coletas").contentType(MediaType.APPLICATION_JSON)
				.content(coleta(carga(1003, ""))))
				.andReturn().getResponse().getHeader("Location");

		mvc.perform(get(location))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cargas[0].numeroNotaFiscal").value(1003));
	}

	@Test
	void retorna404ParaColetaInexistente() throws Exception {
		mvc.perform(get("/coletas/999999"))
				.andExpect(status().isNotFound());
	}

	@Test
	void rejeitaNotaFiscalJaCadastrada() throws Exception {
		String body = coleta(carga(1004, ""));
		mvc.perform(post("/coletas").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated());

		mvc.perform(post("/coletas").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("já cadastrada")));
	}

	@Test
	void rejeitaNotaFiscalRepetidaNaMesmaRequisicao() throws Exception {
		mvc.perform(post("/coletas").contentType(MediaType.APPLICATION_JSON)
				.content(coleta(carga(1005, "") + "," + carga(1005, ""))))
				.andExpect(status().isUnprocessableContent());
	}

	@Test
	void rejeitaDadosInvalidos() throws Exception {
		mvc.perform(post("/coletas").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"clienteNome": "", "clienteCnpj": "123", "cargas": []}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros.clienteNome").exists())
				.andExpect(jsonPath("$.erros.clienteCnpj").exists())
				.andExpect(jsonPath("$.erros.dataProgramada").exists())
				.andExpect(jsonPath("$.erros.cargas").exists());
	}

	private String criarColeta(int numeroNf) throws Exception {
		return mvc.perform(post("/coletas").contentType(MediaType.APPLICATION_JSON)
				.content(coleta(carga(numeroNf, ""))))
				.andReturn().getResponse().getHeader("Location");
	}

	private static String situacao(String situacao) {
		return "{\"situacao\": \"%s\"}".formatted(situacao);
	}

	private void entregarPrimeiraCarga(String locationColeta) throws Exception {
		String coleta = mvc.perform(get(locationColeta)).andReturn().getResponse().getContentAsString();
		Number idCarga = JsonPath.read(coleta, "$.cargas[0].id");
		for (String status : new String[] { "COLETADA", "EM_TRANSITO", "ENTREGUE" }) {
			mvc.perform(patch("/cargas/{id}/status", idCarga).contentType(MediaType.APPLICATION_JSON)
					.content("{\"status\": \"%s\"}".formatted(status)))
					.andExpect(status().isOk());
		}
	}

	@Test
	void alteraSituacaoSeguindoOFluxo() throws Exception {
		String location = criarColeta(1101);

		mvc.perform(patch(location + "/situacao").contentType(MediaType.APPLICATION_JSON)
				.content(situacao("EM_ANDAMENTO")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.situacao").value("EM_ANDAMENTO"));

		entregarPrimeiraCarga(location);

		mvc.perform(patch(location + "/situacao").contentType(MediaType.APPLICATION_JSON)
				.content(situacao("CONCLUIDA")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.situacao").value("CONCLUIDA"));

		mvc.perform(get(location)).andExpect(jsonPath("$.situacao").value("CONCLUIDA"));
	}

	@Test
	void naoConcluiColetaComCargaPendente() throws Exception {
		String location = criarColeta(1105);
		mvc.perform(patch(location + "/situacao").contentType(MediaType.APPLICATION_JSON)
				.content(situacao("EM_ANDAMENTO"))).andExpect(status().isOk());

		mvc.perform(patch(location + "/situacao").contentType(MediaType.APPLICATION_JSON)
				.content(situacao("CONCLUIDA")))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value(
						org.hamcrest.Matchers.containsString("1105 (AGUARDANDO_COLETA)")));

		mvc.perform(get(location)).andExpect(jsonPath("$.situacao").value("EM_ANDAMENTO"));
	}

	@Test
	void rejeitaTransicaoDeSituacaoInvalida() throws Exception {
		String location = criarColeta(1102);

		mvc.perform(patch(location + "/situacao").contentType(MediaType.APPLICATION_JSON)
				.content(situacao("CONCLUIDA")))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value(
						org.hamcrest.Matchers.containsString("Transição de situação inválida")));
	}

	@Test
	void situacaoFinalNaoMuda() throws Exception {
		String location = criarColeta(1103);
		mvc.perform(patch(location + "/situacao").contentType(MediaType.APPLICATION_JSON)
				.content(situacao("CANCELADA"))).andExpect(status().isOk());

		mvc.perform(patch(location + "/situacao").contentType(MediaType.APPLICATION_JSON)
				.content(situacao("PROGRAMADA")))
				.andExpect(status().isUnprocessableContent());
	}

	@Test
	void validaSituacaoNaAlteracao() throws Exception {
		String location = criarColeta(1104);

		mvc.perform(patch(location + "/situacao").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros.situacao").exists());

		mvc.perform(patch("/coletas/999999/situacao").contentType(MediaType.APPLICATION_JSON)
				.content(situacao("CANCELADA")))
				.andExpect(status().isNotFound());
	}
}
