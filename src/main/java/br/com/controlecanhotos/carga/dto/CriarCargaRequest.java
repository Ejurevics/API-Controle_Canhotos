package br.com.controlecanhotos.carga.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CriarCargaRequest(
		@Pattern(regexp = "\\d{14}", message = "CNPJ deve ter 14 dígitos") String emitenteCnpj,
		@Min(0) @Max(999) Short serieNotaFiscal,
		@NotNull @Min(1) @Max(999999999) Integer numeroNotaFiscal,
		@NotNull @Positive @DecimalMax("9999999.999") BigDecimal pesoKg,
		@NotBlank @Size(max = 100) String cidadeDestino,
		@NotNull @Pattern(regexp = "[A-Z]{2}", message = "UF deve ter 2 letras maiúsculas") String ufDestino) {
}
