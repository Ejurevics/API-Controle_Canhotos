package br.com.controlecanhotos.usuario.dto;

import java.time.OffsetDateTime;

import br.com.controlecanhotos.usuario.Perfil;
import br.com.controlecanhotos.usuario.Usuario;

/** Nunca expõe o hash da senha. */
public record UsuarioResponse(
		Long id,
		String login,
		String nome,
		Perfil perfil,
		boolean ativo,
		OffsetDateTime criadoEm,
		OffsetDateTime atualizadoEm) {

	public static UsuarioResponse de(Usuario usuario) {
		return new UsuarioResponse(usuario.getId(), usuario.getLogin(), usuario.getNome(), usuario.getPerfil(),
				usuario.isAtivo(), usuario.getCriadoEm(), usuario.getAtualizadoEm());
	}
}
