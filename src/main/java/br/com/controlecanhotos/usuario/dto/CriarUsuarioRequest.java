package br.com.controlecanhotos.usuario.dto;

import br.com.controlecanhotos.usuario.Perfil;
import br.com.controlecanhotos.usuario.Usuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** O login é normalizado (sem espaços nas pontas, minúsculo) antes de ser gravado. */
public record CriarUsuarioRequest(
		@NotNull @Pattern(regexp = "\s*[A-Za-z0-9._-]{3,50}\s*",
				message = "deve ter de 3 a 50 caracteres: letras, números, ponto, hífen ou sublinhado") String login,
		@NotBlank @Size(max = 150) String nome,
		@NotNull @Size(min = Usuario.TAMANHO_MINIMO_SENHA, max = Usuario.TAMANHO_MAXIMO_SENHA) String senha,
		@NotNull Perfil perfil) {
}
