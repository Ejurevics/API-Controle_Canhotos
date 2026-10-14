package br.com.controlecanhotos.carga.dto;

import br.com.controlecanhotos.carga.dto.validacao.ChaveAcessoCteValida;
import jakarta.validation.constraints.NotNull;

public record CorrigirChaveAcessoRequest(@NotNull @ChaveAcessoCteValida String chaveAcesso) {
}
