package br.com.controlecanhotos.carga.dto;

import java.util.List;

import br.com.controlecanhotos.carga.dto.validacao.ChaveAcessoCteValida;
import br.com.controlecanhotos.notafiscal.dto.CriarNotaFiscalRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CriarCargaRequest(
		@NotNull @ChaveAcessoCteValida String chaveAcesso,
		@NotBlank @Size(max = 100) String cidadeDestino,
		@NotNull @Pattern(regexp = "[A-Z]{2}", message = "UF deve ter 2 letras maiúsculas") String ufDestino,
		@NotEmpty @Valid List<CriarNotaFiscalRequest> notasFiscais) {
}
