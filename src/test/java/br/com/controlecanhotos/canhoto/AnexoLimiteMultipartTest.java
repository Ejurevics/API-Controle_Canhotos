package br.com.controlecanhotos.canhoto;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import com.jayway.jsonpath.JsonPath;

import br.com.controlecanhotos.TestcontainersConfiguration;

/**
 * O limite do multipart (AnexoConfig) só vale num servidor de verdade: o MockMvc não aplica a configuração do
 * servlet. Acima do limite, o Tomcat recusa o envio antes do controller e a API responde 413.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class AnexoLimiteMultipartTest {

	@LocalServerPort
	private int porta;

	private final HttpClient http = HttpClient.newHttpClient();

	private String token() throws Exception {
		HttpResponse<String> resposta = http.send(HttpRequest.newBuilder(uri("/auth/login"))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString("{\"login\": \"admin\", \"senha\": \"admin12345\"}"))
				.build(), HttpResponse.BodyHandlers.ofString());
		assertThat(resposta.statusCode()).isEqualTo(200);
		return JsonPath.read(resposta.body(), "$.token");
	}

	private URI uri(String caminho) {
		return URI.create("http://localhost:" + porta + caminho);
	}

	@Test
	void arquivoPoucoAcimaDoLimiteResponde413() throws Exception {
		// O pedido cabe no limite da requisição; o arquivo passa do limite por arquivo durante a leitura.
		assertThat(enviar(5 * 1024 * 1024 + 10 * 1024)).isEqualTo(413);
	}

	@Test
	void pedidoBemAcimaDoLimiteResponde413() throws Exception {
		// Recusado pelo tamanho do pedido antes de ler o corpo; o Tomcat descarta o resto (max-swallow-size).
		assertThat(enviar(20 * 1024 * 1024)).isEqualTo(413);
	}

	/*
	 * Sem teste com "Expect: 100-continue": nesse caso o Tomcat responde 413 e fecha a conexão sem descartar o
	 * resto do corpo (Http11Processor.checkExpectationAndResponseStatus), e o cliente pode não receber a resposta.
	 */

	private int enviar(int tamanhoArquivo) throws Exception {
		byte[] arquivo = new byte[tamanhoArquivo];
		String fronteira = "----limite-anexo";
		ByteArrayOutputStream corpo = new ByteArrayOutputStream();
		corpo.writeBytes(("--" + fronteira + "\r\nContent-Disposition: form-data; name=\"arquivo\"; "
				+ "filename=\"grande.pdf\"\r\nContent-Type: application/pdf\r\n\r\n").getBytes(StandardCharsets.UTF_8));
		corpo.writeBytes(arquivo);
		corpo.writeBytes(("\r\n--" + fronteira + "--\r\n").getBytes(StandardCharsets.UTF_8));

		// A nota não precisa existir: o limite é aplicado antes de chegar ao controller.
		HttpResponse<String> resposta = http.send(HttpRequest.newBuilder(uri("/notas-fiscais/1/canhoto/anexos"))
				.timeout(Duration.ofSeconds(20))
				.header("Authorization", "Bearer " + token())
				.header("Content-Type", "multipart/form-data; boundary=" + fronteira)
				.POST(HttpRequest.BodyPublishers.ofByteArray(corpo.toByteArray()))
				.build(), HttpResponse.BodyHandlers.ofString());

		if (resposta.statusCode() == 413) {
			assertThat(resposta.body()).contains("Arquivo maior que o limite permitido");
		}
		return resposta.statusCode();
	}
}
