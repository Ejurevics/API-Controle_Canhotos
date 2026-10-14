package br.com.controlecanhotos.seguranca.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String login, @NotBlank String senha) {
}
