package br.com.controlecanhotos.canhoto;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class TipoAnexoTest {

	static final byte[] JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F' };

	static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 0x0D };

	static final byte[] PDF = "%PDF-1.7\n%âãÏÓ\n".getBytes(StandardCharsets.ISO_8859_1);

	@Test
	void detectaPeloConteudo() {
		assertThat(TipoAnexo.detectar(JPEG)).contains(TipoAnexo.JPEG);
		assertThat(TipoAnexo.detectar(PNG)).contains(TipoAnexo.PNG);
		assertThat(TipoAnexo.detectar(PDF)).contains(TipoAnexo.PDF);
	}

	@Test
	void recusaOutrosTiposEArquivoCurto() {
		assertThat(TipoAnexo.detectar("GIF89a".getBytes(StandardCharsets.US_ASCII))).isEmpty();
		assertThat(TipoAnexo.detectar("<html><script>".getBytes(StandardCharsets.US_ASCII))).isEmpty();
		assertThat(TipoAnexo.detectar(new byte[] { (byte) 0xFF, (byte) 0xD8 })).isEmpty();
		assertThat(TipoAnexo.detectar(new byte[0])).isEmpty();
	}

	@Test
	void tipoDeConteudoEExtensao() {
		assertThat(TipoAnexo.JPEG.getTipoConteudo()).isEqualTo("image/jpeg");
		assertThat(TipoAnexo.PNG.getExtensao()).isEqualTo(".png");
		assertThat(TipoAnexo.PDF.getTipoConteudo()).isEqualTo("application/pdf");
	}
}
