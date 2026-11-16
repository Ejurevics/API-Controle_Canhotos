package br.com.controlecanhotos.canhoto;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

/**
 * Tipos de arquivo aceitos como comprovante do canhoto (ADR 0006). O tipo vem da assinatura no início do
 * conteúdo, não do Content-Type nem da extensão informados pelo cliente.
 */
public enum TipoAnexo {

	JPEG("image/jpeg", ".jpg", new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF }),
	PNG("image/png", ".png", new byte[] { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' }),
	PDF("application/pdf", ".pdf", "%PDF-".getBytes(StandardCharsets.US_ASCII));

	private final String tipoConteudo;

	private final String extensao;

	private final byte[] assinatura;

	TipoAnexo(String tipoConteudo, String extensao, byte[] assinatura) {
		this.tipoConteudo = tipoConteudo;
		this.extensao = extensao;
		this.assinatura = assinatura;
	}

	public static Optional<TipoAnexo> detectar(byte[] conteudo) {
		return Arrays.stream(values()).filter(tipo -> tipo.comecaCom(conteudo)).findFirst();
	}

	private boolean comecaCom(byte[] conteudo) {
		return conteudo.length >= assinatura.length
				&& Arrays.equals(conteudo, 0, assinatura.length, assinatura, 0, assinatura.length);
	}

	public String getTipoConteudo() {
		return tipoConteudo;
	}

	public String getExtensao() {
		return extensao;
	}
}
