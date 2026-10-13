package br.com.controlecanhotos.coleta.dto;

import java.time.LocalDate;
import java.util.List;

import br.com.controlecanhotos.carga.dto.CriarCargaRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CriarColetaRequest(
		@NotBlank @Size(max = 150) String clienteNome,
		@NotNull @Pattern(regexp = "\\d{14}", message = "CNPJ deve ter 14 dígitos") String clienteCnpj,
		@NotNull LocalDate dataProgramada,
		@NotEmpty @Valid List<CriarCargaRequest> cargas) {
}
