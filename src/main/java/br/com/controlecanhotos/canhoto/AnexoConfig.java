package br.com.controlecanhotos.canhoto;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import jakarta.servlet.MultipartConfigElement;

@Configuration
@EnableConfigurationProperties(PropriedadesAnexo.class)
public class AnexoConfig {

	/** Folga para os cabeçalhos do multipart, além do arquivo. */
	private static final DataSize FOLGA_REQUISICAO = DataSize.ofKilobytes(64);

	/**
	 * O multipart usa o mesmo limite do anexo ({@code ANEXO_TAMANHO_MAXIMO}), para recusar o envio antes de
	 * ler o arquivo inteiro. Substitui a configuração padrão do Spring Boot (1 MB por arquivo).
	 */
	@Bean
	MultipartConfigElement multipartConfigElement(PropriedadesAnexo propriedades) {
		MultipartConfigFactory fabrica = new MultipartConfigFactory();
		fabrica.setMaxFileSize(propriedades.tamanhoMaximo());
		fabrica.setMaxRequestSize(
				DataSize.ofBytes(propriedades.tamanhoMaximo().toBytes() + FOLGA_REQUISICAO.toBytes()));
		return fabrica.createMultipartConfig();
	}
}
