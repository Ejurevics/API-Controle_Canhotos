package br.com.controlecanhotos.usuario.dto;

import java.time.OffsetDateTime;
import java.util.List;

import br.com.controlecanhotos.historico.HistoricoUsuario;
import br.com.controlecanhotos.usuario.Perfil;
import br.com.controlecanhotos.usuario.Usuario;

/** {@link UsuarioResponse} com o histórico da gestão; só em {@code GET /usuarios/{id}}. */
public record UsuarioDetalheResponse(
		Long id,
		String login,
		String nome,
		Perfil perfil,
		boolean ativo,
		OffsetDateTime criadoEm,
		OffsetDateTime atualizadoEm,
		List<Historico> historico) {

	public record Historico(HistoricoUsuario.Acao acao, String detalhe, OffsetDateTime alteradoEm,
			String alteradoPor) {

		static Historico de(HistoricoUsuario h) {
			return new Historico(h.getAcao(), h.getDetalhe(), h.getAlteradoEm(), h.getAlteradoPor());
		}
	}

	public static UsuarioDetalheResponse de(Usuario usuario) {
		return new UsuarioDetalheResponse(usuario.getId(), usuario.getLogin(), usuario.getNome(),
				usuario.getPerfil(), usuario.isAtivo(), usuario.getCriadoEm(), usuario.getAtualizadoEm(),
				usuario.getHistorico().stream().map(Historico::de).toList());
	}
}
