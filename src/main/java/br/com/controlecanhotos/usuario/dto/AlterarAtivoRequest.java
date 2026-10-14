package br.com.controlecanhotos.usuario.dto;

import jakarta.validation.constraints.NotNull;

public record AlterarAtivoRequest(@NotNull Boolean ativo) {
}
