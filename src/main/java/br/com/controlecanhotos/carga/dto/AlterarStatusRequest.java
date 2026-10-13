package br.com.controlecanhotos.carga.dto;

import br.com.controlecanhotos.carga.StatusCarga;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AlterarStatusRequest(
		@NotNull StatusCarga status,
		@Size(max = 255) String motivo) {
}
