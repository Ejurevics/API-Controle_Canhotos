package br.com.controlecanhotos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Roda a V7 (histórico de situação da coleta) e a V8 (correção da chave) sobre dados criados no esquema da V6,
 * como num banco já em uso. Usa um banco só dele: a migração acontece uma vez, em {@link #migrar()}.
 */
class MigracaoHistoricoColetaTest {

	private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
			DockerImageName.parse("postgres:18-alpine"));

	private static long programada;
	private static long emAndamento;
	private static long carga;

	@BeforeAll
	static void migrar() throws SQLException {
		POSTGRES.start();

		flyway("6").migrate();
		try (Connection conexao = conexao()) {
			programada = inserir(conexao, coletaSql("PROGRAMADA", "2026-10-01T10:00:00Z"));
			emAndamento = inserir(conexao, coletaSql("EM_ANDAMENTO", "2026-10-03T15:30:00Z"));
			carga = inserir(conexao, """
					INSERT INTO carga (coleta_id, chave_acesso, chave_provisoria, cidade_destino, uf_destino, status)
					VALUES (%d, '99' || lpad('1', 42, '0'), TRUE, 'Campinas', 'SP', 'EM_TRANSITO') RETURNING id"""
					.formatted(programada));
		}

		flyway(null).migrate();
	}

	@AfterAll
	static void parar() {
		POSTGRES.stop();
	}

	private static String coletaSql(String situacao, String atualizadoEm) {
		return """
				INSERT INTO coleta (cliente_nome, cliente_cnpj, data_programada, situacao, atualizado_em)
				VALUES ('Cliente A', '11111111000111', '2026-10-01', '%s', '%s') RETURNING id"""
				.formatted(situacao, atualizadoEm);
	}

	private static Flyway flyway(String alvo) {
		var configuracao = Flyway.configure()
				.dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
				.locations("classpath:db/migration");
		if (alvo != null) {
			configuracao.target(alvo);
		}
		return configuracao.load();
	}

	private static Connection conexao() throws SQLException {
		return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
	}

	private static long inserir(Connection conexao, String sql) throws SQLException {
		try (Statement statement = conexao.createStatement(); ResultSet resultado = statement.executeQuery(sql)) {
			resultado.next();
			return resultado.getLong(1);
		}
	}

	private static List<String> consultar(String sql) throws SQLException {
		List<String> linhas = new ArrayList<>();
		try (Connection conexao = conexao(); Statement statement = conexao.createStatement();
				ResultSet resultado = statement.executeQuery(sql)) {
			int colunas = resultado.getMetaData().getColumnCount();
			while (resultado.next()) {
				List<String> campos = new ArrayList<>();
				for (int i = 1; i <= colunas; i++) {
					campos.add(String.valueOf(resultado.getString(i)));
				}
				linhas.add(String.join("|", campos));
			}
		}
		return linhas;
	}

	@Test
	void cadaColetaGanhaUmRegistroComASituacaoAtualSemUsuario() throws SQLException {
		assertThat(consultar("""
				SELECT coleta_id, situacao_anterior, situacao_nova, alterado_em AT TIME ZONE 'UTC', alterado_por
				FROM historico_situacao_coleta ORDER BY coleta_id"""))
				.containsExactly(
						programada + "|null|PROGRAMADA|2026-10-01 10:00:00|null",
						emAndamento + "|null|EM_ANDAMENTO|2026-10-03 15:30:00|null");
	}

	@Test
	void cargaJaExistenteFicaSemCorrecaoDeChave() throws SQLException {
		assertThat(consultar("""
				SELECT chave_provisoria, chave_provisoria_anterior, chave_corrigida_em, chave_corrigida_por
				FROM carga WHERE id = %d""".formatted(carga)))
				.containsExactly("t|null|null|null");
	}

	@Test
	void correcaoDaChavePrecisaDaChaveAnteriorEDaData() {
		assertThatThrownBy(() -> {
			try (Connection conexao = conexao(); Statement statement = conexao.createStatement()) {
				statement.execute("UPDATE carga SET chave_corrigida_em = now() WHERE id = " + carga);
			}
		}).isInstanceOf(SQLException.class).hasMessageContaining("ck_carga_correcao_chave");
	}
}
