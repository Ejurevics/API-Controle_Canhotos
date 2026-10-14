package br.com.controlecanhotos.usuario.dto;

import br.com.controlecanhotos.usuario.Perfil;
import jakarta.validation.constraints.NotNull;

public record AlterarPerfilRequest(@NotNull Perfil perfil) {
}
