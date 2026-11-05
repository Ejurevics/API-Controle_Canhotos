package br.com.controlecanhotos.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.Test;

import br.com.controlecanhotos.historico.HistoricoUsuario;
import br.com.controlecanhotos.historico.HistoricoUsuario.Acao;

class UsuarioTest {

	private static Usuario operador() {
		return new Usuario("maria", "Maria", "hash", Perfil.OPERADOR);
	}

	@Test
	void registraCadaAcaoDaGestaoNoHistorico() {
		Usuario maria = operador();

		maria.alterarPerfil(Perfil.ADMIN);
		maria.redefinirSenha("hash-2");
		maria.desativar();
		maria.ativar();
		maria.trocarSenha("hash-3");

		assertThat(maria.getHistorico())
				.extracting(HistoricoUsuario::getAcao, HistoricoUsuario::getDetalhe)
				.containsExactly(
						tuple(Acao.CRIADO, null),
						tuple(Acao.PERFIL_ALTERADO, "OPERADOR → ADMIN"),
						tuple(Acao.SENHA_REDEFINIDA, null),
						tuple(Acao.DESATIVADO, null),
						tuple(Acao.REATIVADO, null),
						tuple(Acao.SENHA_TROCADA, null));
	}

	@Test
	void pedidoQueNaoMudaNadaNaoEntraNoHistoricoNemDerrubaTokens() {
		Usuario maria = operador();

		maria.ativar();
		maria.alterarPerfil(Perfil.OPERADOR);
		maria.desativar();
		maria.desativar();

		assertThat(maria.getHistorico()).extracting(HistoricoUsuario::getAcao)
				.containsExactly(Acao.CRIADO, Acao.DESATIVADO);
		assertThat(maria.getVersaoCredencial()).isEqualTo(1);
	}
}
