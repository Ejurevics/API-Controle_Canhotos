package br.com.controlecanhotos.coleta.dto;

import br.com.controlecanhotos.coleta.SituacaoColeta;
import jakarta.validation.constraints.NotNull;

public record AlterarSituacaoRequest(@NotNull SituacaoColeta situacao) {
}
