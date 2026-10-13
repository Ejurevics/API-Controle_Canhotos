package br.com.controlecanhotos.canhoto.dto;

import java.time.LocalDate;

import br.com.controlecanhotos.canhoto.StatusCanhoto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistrarCanhotoRequest(
		@NotNull StatusCanhoto status,
		@NotNull LocalDate dataRecebimento,
		@Size(max = 500) String observacao,
		Boolean dataFuturaConfirmada) {
}
