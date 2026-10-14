package br.com.controlecanhotos.notafiscal.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record CriarNotaFiscalRequest(
		@Pattern(regexp = "\\d{14}", message = "CNPJ deve ter 14 dígitos") String emitenteCnpj,
		@Min(0) @Max(999) Short serieNotaFiscal,
		@NotNull @Min(1) @Max(999999999) Integer numeroNotaFiscal,
		@NotNull @Positive @DecimalMax("9999999.999") BigDecimal pesoKg) {
}
