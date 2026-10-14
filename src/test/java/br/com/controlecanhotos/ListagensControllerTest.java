package br.com.controlecanhotos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

/**
 * Cada teste usa um CNPJ de cliente exclusivo, porque o banco é compartilhado entre as classes de teste.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ListagensControllerTest {

	@Autowired
	private MockMvc mvc;

	private Long criarColeta(String cnpj, String dataProgramada, int... numerosNf) throws Exception {
		StringBuilder cargas = new StringBuilder();
		for (int numero : numerosNf) {
			if (!cargas.isEmpty()) {
				cargas.append(",");
			}
			cargas.append("""
					{"numeroNotaFiscal": %d, "pesoKg": 10, "cidadeDestino": "Campinas", "ufDestino": "SP"}
					""".formatted(numero));
		}
		String body = """
				{"clienteNome": "Cliente", "clienteCnpj": "%s", "dataProgramada": "%s", "cargas": [%s]}
				""".formatted(cnpj, dataProgramada, cargas);
		String resposta = mvc.perform(post("/coletas").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(resposta, "$.id")).longValue();
	}

	private void alterarStatusCarga(long cargaId, String status) throws Exception {
		mvc.perform(patch("/cargas/" + cargaId + "/status").contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\": \"%s\"}".formatted(status)))
				.andExpect(status().isOk());
	}

	private long primeiraCargaDaColeta(long coletaId) throws Exception {
		String resposta = mvc.perform(get("/coletas/" + coletaId))
				.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(resposta, "$.cargas[0].id")).longValue();
	}

	private void entregarEReceber(long cargaId) throws Exception {
		alterarStatusCarga(cargaId, "COLETADA");
		alterarStatusCarga(cargaId, "EM_TRANSITO");
		alterarStatusCarga(cargaId, "ENTREGUE");
		mvc.perform(post("/cargas/" + cargaId + "/canhoto/registros").contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\": \"RECEBIDO\", \"dataRecebimento\": \"2026-01-01\"}"))
				.andExpect(status().isOk());
	}

	@Test
	void listaColetasDoClienteOrdenadasPelaDataProgramadaDecrescente() throws Exception {
		String cnpj = "30000000000001";
		criarColeta(cnpj, "2026-11-01", 20001);
		criarColeta(cnpj, "2026-11-03", 20002);
		criarColeta(cnpj, "2026-11-02", 20003);
		criarColeta("30000000000099", "2026-11-04", 20004);

		mvc.perform(get("/coletas").param("clienteCnpj", cnpj))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.page.totalElements").value(3))
				.andExpect(jsonPath("$.content[0].dataProgramada").value("2026-11-03"))
				.andExpect(jsonPath("$.content[1].dataProgramada").value("2026-11-02"))
				.andExpect(jsonPath("$.content[2].dataProgramada").value("2026-11-01"));
	}

	@Test
	void paginaAsColetas() throws Exception {
		String cnpj = "30000000000002";
		criarColeta(cnpj, "2026-11-01", 20011);
		criarColeta(cnpj, "2026-11-02", 20012);
		criarColeta(cnpj, "2026-11-03", 20013);

		mvc.perform(get("/coletas").param("clienteCnpj", cnpj).param("tamanho", "2").param("pagina", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.page.totalElements").value(3))
				.andExpect(jsonPath("$.page.totalPages").value(2))
				.andExpect(jsonPath("$.page.number").value(1));
	}

	@Test
	void filtraColetasPorSituacaoEPeriodo() throws Exception {
		String cnpj = "30000000000003";
		criarColeta(cnpj, "2026-11-01", 20021);
		Long meio = criarColeta(cnpj, "2026-11-10", 20022);
		criarColeta(cnpj, "2026-11-20", 20023);
		mvc.perform(patch("/coletas/" + meio + "/situacao").contentType(MediaType.APPLICATION_JSON)
				.content("{\"situacao\": \"EM_ANDAMENTO\"}"))
				.andExpect(status().isOk());

		mvc.perform(get("/coletas").param("clienteCnpj", cnpj).param("situacao", "EM_ANDAMENTO"))
				.andExpect(jsonPath("$.page.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].id").value(meio));

		mvc.perform(get("/coletas").param("clienteCnpj", cnpj)
				.param("dataInicio", "2026-11-05").param("dataFim", "2026-11-15"))
				.andExpect(jsonPath("$.page.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].id").value(meio));

		mvc.perform(get("/coletas").param("clienteCnpj", cnpj).param("dataInicio", "2026-11-10"))
				.andExpect(jsonPath("$.page.totalElements").value(2));
	}

	@Test
	void rejeitaParametrosInvalidosNaListagem() throws Exception {
		mvc.perform(get("/coletas").param("dataInicio", "2026-11-10").param("dataFim", "2026-11-01"))
				.andExpect(status().isUnprocessableContent());
		mvc.perform(get("/coletas").param("tamanho", "0"))
				.andExpect(status().isUnprocessableContent());
		mvc.perform(get("/coletas").param("tamanho", "101"))
				.andExpect(status().isUnprocessableContent());
		mvc.perform(get("/coletas").param("pagina", "-1"))
				.andExpect(status().isUnprocessableContent());
		mvc.perform(get("/coletas").param("situacao", "INEXISTENTE"))
				.andExpect(status().isBadRequest());
		mvc.perform(get("/coletas").param("dataInicio", "ontem"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void filtraCargasPorStatusEColeta() throws Exception {
		Long coleta = criarColeta("30000000000004", "2026-11-01", 20031, 20032, 20033);
		long cargaId = primeiraCargaDaColeta(coleta);
		alterarStatusCarga(cargaId, "COLETADA");

		mvc.perform(get("/cargas").param("coletaId", coleta.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.page.totalElements").value(3));

		mvc.perform(get("/cargas").param("coletaId", coleta.toString()).param("status", "COLETADA"))
				.andExpect(jsonPath("$.page.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].id").value(cargaId))
				.andExpect(jsonPath("$.content[0].statusCanhoto").value("PENDENTE"));

		mvc.perform(get("/cargas").param("coletaId", coleta.toString()).param("status", "AGUARDANDO_COLETA"))
				.andExpect(jsonPath("$.page.totalElements").value(2));
	}

	@Test
	void listaCanhotosPendentesPorClienteEPeriodo() throws Exception {
		String cnpj = "30000000000005";
		Long coletaAntiga = criarColeta(cnpj, "2026-11-01", 20041, 20042);
		criarColeta(cnpj, "2026-12-01", 20043);
		criarColeta("30000000000098", "2026-11-01", 20044);
		entregarEReceber(primeiraCargaDaColeta(coletaAntiga));

		mvc.perform(get("/canhotos").param("status", "PENDENTE").param("clienteCnpj", cnpj))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.page.totalElements").value(2))
				.andExpect(jsonPath("$.content[0].clienteCnpj").value(cnpj))
				.andExpect(jsonPath("$.content[0].status").value("PENDENTE"));

		mvc.perform(get("/canhotos").param("status", "RECEBIDO").param("clienteCnpj", cnpj))
				.andExpect(jsonPath("$.page.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].dataRecebimento").value("2026-01-01"));

		mvc.perform(get("/canhotos").param("status", "PENDENTE").param("clienteCnpj", cnpj)
				.param("dataInicio", "2026-11-15").param("dataFim", "2026-12-31"))
				.andExpect(jsonPath("$.page.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].numeroNotaFiscal").value(20043))
				.andExpect(jsonPath("$.content[0].dataProgramada").value("2026-12-01"));

		mvc.perform(get("/canhotos").param("clienteCnpj", cnpj))
				.andExpect(jsonPath("$.page.totalElements").value(3));
	}
}
