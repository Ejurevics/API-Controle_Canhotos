package br.com.controlecanhotos.usuario.dto;

import br.com.controlecanhotos.usuario.Usuario;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RedefinirSenhaRequest(
		@NotNull @Size(min = Usuario.TAMANHO_MINIMO_SENHA, max = Usuario.TAMANHO_MAXIMO_SENHA) String novaSenha) {
}
