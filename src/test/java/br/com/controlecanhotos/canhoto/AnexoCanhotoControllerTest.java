package br.com.controlecanhotos.canhoto;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import br.com.controlecanhotos.AutenticadoComoAdmin;
import br.com.controlecanhotos.Cte;
import br.com.controlecanhotos.TestcontainersConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, AutenticadoComoAdmin.class })
class AnexoCanhotoControllerTest {

	private static final AtomicInteger NUMERO_NF = new AtomicInteger(6000);

	@Autowired
	private MockMvc mvc;

	private long criarNota(String... statusSequencia) throws Exception {
		int numeroNf = NUMERO_NF.incrementAndGet();
		String body = """
				{"clienteNome": "Cliente D", "clienteCnpj": "55555555000155", "dataProgramada": "2026-11-16",
				 "cargas": [{"chaveAcesso": "%s", "cidadeDestino": "Sorocaba", "ufDestino": "SP",
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

	private ResultActions enviar(long notaId, String nome, String tipoDeclarado, byte[] conteudo) throws Exception {
		return mvc.perform(multipart("/notas-fiscais/{id}/canhoto/anexos", notaId)
				.file(new MockMultipartFile("arquivo", nome, tipoDeclarado, conteudo)));
	}

	private static long id(ResultActions resultado) throws Exception {
		return ((Number) JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$.id"))
				.longValue();
	}

	@Test
	void anexaListaNoCanhotoEBaixa() throws Exception {
		long nota = criarNotaEntregue();

		ResultActions envio = enviar(nota, "canhoto 1001.jpg", "image/jpeg", TipoAnexoTest.JPEG)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.nomeArquivo").value("canhoto 1001.jpg"))
				.andExpect(jsonPath("$.tipoConteudo").value("image/jpeg"))
				.andExpect(jsonPath("$.tamanhoBytes").value(TipoAnexoTest.JPEG.length))
				.andExpect(jsonPath("$.enviadoEm").exists())
				.andExpect(jsonPath("$.enviadoPor").value(AutenticadoComoAdmin.LOGIN));
		long anexo = id(envio);
		envio.andExpect(header().string("Location",
				"/notas-fiscais/%d/canhoto/anexos/%d/arquivo".formatted(nota, anexo)));
		enviar(nota, "verso.pdf", "application/pdf", TipoAnexoTest.PDF).andExpect(status().isCreated());

		mvc.perform(get("/notas-fiscais/{id}/canhoto", nota))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PENDENTE"))
				.andExpect(jsonPath("$.anexos.length()").value(2))
				.andExpect(jsonPath("$.anexos[0].id").value(anexo))
				.andExpect(jsonPath("$.anexos[1].tipoConteudo").value("application/pdf"));

		mvc.perform(get("/notas-fiscais/{id}/canhoto/anexos/{anexo}/arquivo", nota, anexo))
				.andExpect(status().isOk())
				.andExpect(content().contentType("image/jpeg"))
				.andExpect(content().bytes(TipoAnexoTest.JPEG))
				.andExpect(header().string("Content-Disposition", containsString("attachment")))
				.andExpect(header().string("Content-Disposition", containsString("canhoto%201001.jpg")))
				.andExpect(header().string("X-Content-Type-Options", "nosniff"));
	}

	@Test
	void tipoVemDoConteudoENaoDoDeclarado() throws Exception {
		long nota = criarNotaEntregue();

		long anexo = id(enviar(nota, "foto.jpg", "image/jpeg", TipoAnexoTest.PNG)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tipoConteudo").value("image/png")));

		mvc.perform(get("/notas-fiscais/{id}/canhoto/anexos/{anexo}/arquivo", nota, anexo))
				.andExpect(content().contentType("image/png"));
	}

	@Test
	void recusaTipoNaoAceito() throws Exception {
		long nota = criarNotaEntregue();

		enviar(nota, "pagina.pdf", "application/pdf", "<html><script>alert(1)</script>".getBytes())
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value(containsString("JPEG, PNG ou PDF")));
	}

	@Test
	void exigeCargaEntregue() throws Exception {
		long nota = criarNota("COLETADA");

		enviar(nota, "canhoto.jpg", "image/jpeg", TipoAnexoTest.JPEG)
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value(containsString("carga ENTREGUE")));
	}

	@Test
	void recusaArquivoRepetidoNoMesmoCanhoto() throws Exception {
		long nota = criarNotaEntregue();
		long anexo = id(enviar(nota, "frente.jpg", "image/jpeg", TipoAnexoTest.JPEG));

		enviar(nota, "de-novo.jpg", "image/jpeg", TipoAnexoTest.JPEG)
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value("Este arquivo já está anexado ao canhoto (anexo %d)"
						.formatted(anexo)));

		// Em outro canhoto, o mesmo arquivo entra.
		enviar(criarNotaEntregue(), "frente.jpg", "image/jpeg", TipoAnexoTest.JPEG)
				.andExpect(status().isCreated());
	}

	@Test
	void recusaArquivoAcimaDoLimite() throws Exception {
		long nota = criarNotaEntregue();
		byte[] grande = new byte[5 * 1024 * 1024 + 1];
		System.arraycopy(TipoAnexoTest.PDF, 0, grande, 0, TipoAnexoTest.PDF.length);

		enviar(nota, "grande.pdf", "application/pdf", grande)
				.andExpect(status().isContentTooLarge())
				.andExpect(jsonPath("$.detail").value(containsString("passa do limite")));
	}

	@Test
	void exigeOArquivo() throws Exception {
		long nota = criarNotaEntregue();

		mvc.perform(multipart("/notas-fiscais/{id}/canhoto/anexos", nota)
				.file(new MockMultipartFile("outro", "a.jpg", "image/jpeg", TipoAnexoTest.JPEG)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void anexoDeOutraNotaNaoEEncontrado() throws Exception {
		long nota = criarNotaEntregue();
		long anexo = id(enviar(nota, "canhoto.jpg", "image/jpeg", TipoAnexoTest.JPEG));
		long outraNota = criarNotaEntregue();

		mvc.perform(get("/notas-fiscais/{id}/canhoto/anexos/{anexo}/arquivo", outraNota, anexo))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Anexo não encontrado: " + anexo));
		mvc.perform(get("/notas-fiscais/{id}/canhoto/anexos/{anexo}/arquivo", 999999, anexo))
				.andExpect(status().isNotFound());
	}
}
