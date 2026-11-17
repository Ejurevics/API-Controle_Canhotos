package br.com.controlecanhotos.canhoto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RemoverAnexoRequest(@NotBlank @Size(max = 500) String motivo) {
}
