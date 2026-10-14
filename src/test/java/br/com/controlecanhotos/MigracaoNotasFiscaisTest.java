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
 * Roda a V3 (agrupar NFs por CT-e) sobre dados criados no esquema da V2, como num banco já em uso.
 * Usa um banco só dele: a migração acontece uma vez, em {@link #migrar()}.
 */
class MigracaoNotasFiscaisTest {

	private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
			DockerImageName.parse("postgres:18-alpine"));

	private static long carga1;
	private static long carga2;
	private static long canhoto1;

	@BeforeAll
	static void migrar() throws SQLException {
		POSTGRES.start();

		flyway("2").migrate();
		try (Connection conexao = conexao()) {
			long coleta = inserir(conexao, """
					INSERT INTO coleta (cliente_nome, cliente_cnpj, data_programada, situacao)
					VALUES ('Cliente A', '11111111000111', '2026-10-01', 'EM_ANDAMENTO') RETURNING id""");
			carga1 = inserir(conexao, cargaSql(coleta, 100, "10.500", "ENTREGUE"));
			carga2 = inserir(conexao, cargaSql(coleta, 200, "20.000", "EM_TRANSITO"));
			canhoto1 = inserir(conexao, """
					INSERT INTO canhoto (carga_id, status, data_recebimento, observacao)
					VALUES (%d, 'DIVERGENTE', '2026-10-05', 'Rasurado') RETURNING id""".formatted(carga1));
			inserir(conexao, """
					INSERT INTO canhoto (carga_id, status) VALUES (%d, 'PENDENTE') RETURNING id""".formatted(carga2));
			executar(conexao, """
					INSERT INTO historico_canhoto (canhoto_id, status_novo) VALUES (%d, 'PENDENTE')
					""".formatted(canhoto1));
			executar(conexao, """
					INSERT INTO historico_status (carga_id, status_novo, motivo)
					VALUES (%d, 'AGUARDANDO_COLETA', 'Carga registrada'), (%d, 'AGUARDANDO_COLETA', 'Carga registrada')
					""".formatted(carga1, carga2));
		}

		flyway(null).migrate();
	}

	@AfterAll
	static void parar() {
		POSTGRES.stop();
	}

	private static String cargaSql(long coleta, int numeroNf, String peso, String status) {
		return """
				INSERT INTO carga (coleta_id, emitente_cnpj, serie_nota_fiscal, numero_nota_fiscal, peso_kg,
				                   cidade_destino, uf_destino, status)
				VALUES (%d, '11111111000111', 1, %d, %s, 'Campinas', 'SP', '%s') RETURNING id"""
				.formatted(coleta, numeroNf, peso, status);
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

	private static void executar(Connection conexao, String sql) throws SQLException {
		try (Statement statement = conexao.createStatement()) {
			statement.execute(sql);
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
	void cadaCargaViraUmCteComUmaNotaComOsMesmosDados() throws SQLException {
		assertThat(consultar("""
				SELECT carga_id, emitente_cnpj, serie_nota_fiscal, numero_nota_fiscal, peso_kg
				FROM nota_fiscal ORDER BY numero_nota_fiscal"""))
				.containsExactly(
						carga1 + "|11111111000111|1|100|10.500",
						carga2 + "|11111111000111|1|200|20.000");
	}

	@Test
	void canhotoSegueParaANotaDaSuaCargaPreservandoOEstado() throws SQLException {
		assertThat(consultar("""
				SELECT nf.carga_id, nf.numero_nota_fiscal, c.status, c.data_recebimento, c.observacao
				FROM canhoto c JOIN nota_fiscal nf ON nf.id = c.nota_fiscal_id
				ORDER BY nf.numero_nota_fiscal"""))
				.containsExactly(
						carga1 + "|100|DIVERGENTE|2026-10-05|Rasurado",
						carga2 + "|200|PENDENTE|null|null");
	}

	@Test
	void historicosContinuamLigadosAosMesmosRegistros() throws SQLException {
		assertThat(consultar("SELECT canhoto_id FROM historico_canhoto")).containsExactly(String.valueOf(canhoto1));
		assertThat(consultar("SELECT carga_id FROM historico_status ORDER BY carga_id"))
				.containsExactly(String.valueOf(carga1), String.valueOf(carga2));
	}

	@Test
	void cargasAtuaisRecebemChaveProvisoriaDe44Digitos() throws SQLException {
		assertThat(consultar("SELECT id, chave_acesso, chave_provisoria::text FROM carga ORDER BY id"))
				.containsExactly(
						carga1 + "|99" + "%042d".formatted(carga1) + "|true",
						carga2 + "|99" + "%042d".formatted(carga2) + "|true");
	}

	@Test
	void dadosDaNotaSaemDaCargaECargaIdSaiDoCanhoto() throws SQLException {
		assertThat(consultar("""
				SELECT column_name FROM information_schema.columns
				WHERE table_name = 'carga'
				  AND column_name IN ('emitente_cnpj', 'serie_nota_fiscal', 'numero_nota_fiscal', 'peso_kg')"""))
				.isEmpty();
		assertThat(consultar("""
				SELECT column_name FROM information_schema.columns
				WHERE table_name = 'canhoto' AND column_name = 'carga_id'""")).isEmpty();
	}

	@Test
	void mesmaNotaPodeEstarEmOutroCteMasNaoDuasVezesNoMesmo() throws SQLException {
		try (Connection conexao = conexao()) {
			// a nota 100 já existe no CT-e da carga1; agora pode existir também no da carga2
			executar(conexao, notaSql(carga2, 100));

			assertThatThrownBy(() -> executar(conexao, notaSql(carga2, 100)))
					.isInstanceOf(SQLException.class)
					.hasMessageContaining("uk_nota_fiscal_carga_nota");
		}
	}

	@Test
	void chaveDeAcessoDuplicadaOuMalFormadaEhRejeitada() throws SQLException {
		try (Connection conexao = conexao()) {
			assertThatThrownBy(() -> executar(conexao, "UPDATE carga SET chave_acesso = (SELECT chave_acesso "
					+ "FROM carga WHERE id = " + carga1 + ") WHERE id = " + carga2))
					.hasMessageContaining("uk_carga_chave_acesso");
			assertThatThrownBy(() -> executar(conexao,
					"UPDATE carga SET chave_acesso = '123' WHERE id = " + carga2))
					.hasMessageContaining("ck_carga_chave_acesso");
		}
	}

	private static String notaSql(long carga, int numero) {
		return """
				INSERT INTO nota_fiscal (carga_id, emitente_cnpj, serie_nota_fiscal, numero_nota_fiscal, peso_kg)
				VALUES (%d, '11111111000111', 1, %d, 1)""".formatted(carga, numero);
	}
}
