package br.com.controlecanhotos.usuario;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import br.com.controlecanhotos.historico.HistoricoUsuario;
import br.com.controlecanhotos.historico.HistoricoUsuario.Acao;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

@Entity
public class Usuario {

	public static final int TAMANHO_MINIMO_SENHA = 8;

	/** O BCrypt só considera os primeiros 72 bytes da senha (e o Spring recusa senhas maiores). */
	public static final int TAMANHO_MAXIMO_SENHA = 72;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String login;

	private String nome;

	private String senhaHash;

	@Enumerated(EnumType.STRING)
	private Perfil perfil;

	private boolean ativo;

	/** Vai no token; mudar o valor invalida os tokens já emitidos (ADR 0004). */
	private int versaoCredencial;

	@CreationTimestamp
	@Column(updatable = false)
	private OffsetDateTime criadoEm;

	@UpdateTimestamp
	private OffsetDateTime atualizadoEm;

	@OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL)
	@OrderBy("alteradoEm, id")
	private List<HistoricoUsuario> historico = new ArrayList<>();

	protected Usuario() {
	}

	/** {@code senhaHash} já vem codificada (BCrypt); a entidade nunca vê a senha em texto. */
	public Usuario(String login, String nome, String senhaHash, Perfil perfil) {
		this.login = normalizarLogin(login);
		this.nome = nome;
		this.senhaHash = senhaHash;
		this.perfil = perfil;
		this.ativo = true;
		registrar(Acao.CRIADO, null);
	}

	/** Login é guardado e comparado sem espaços nas pontas e em minúsculas. */
	public static String normalizarLogin(String login) {
		return login == null ? null : login.strip().toLowerCase();
	}

	/** Também invalida os tokens já emitidos; reativar não os devolve. Já inativo, não muda nada. */
	public void desativar() {
		if (ativo) {
			ativo = false;
			invalidarTokens();
			registrar(Acao.DESATIVADO, null);
		}
	}

	/** Já ativo, não muda nada. */
	public void ativar() {
		if (!ativo) {
			ativo = true;
			registrar(Acao.REATIVADO, null);
		}
	}

	/** O ADMIN define uma senha nova. {@code senhaHash} já vem codificada (BCrypt). Invalida os tokens já emitidos. */
	public void redefinirSenha(String senhaHash) {
		alterarSenha(senhaHash, Acao.SENHA_REDEFINIDA);
	}

	/** O próprio usuário troca a senha. Invalida os tokens já emitidos, inclusive o atual. */
	public void trocarSenha(String senhaHash) {
		alterarSenha(senhaHash, Acao.SENHA_TROCADA);
	}

	/** Invalida os tokens já emitidos, que levam o perfil antigo; o mesmo perfil não muda nada. */
	public void alterarPerfil(Perfil novoPerfil) {
		if (novoPerfil != perfil) {
			registrar(Acao.PERFIL_ALTERADO, perfil + " → " + novoPerfil);
			perfil = novoPerfil;
			invalidarTokens();
		}
	}

	private void alterarSenha(String senhaHash, Acao acao) {
		this.senhaHash = senhaHash;
		invalidarTokens();
		registrar(acao, null);
	}

	private void registrar(Acao acao, String detalhe) {
		historico.add(new HistoricoUsuario(this, acao, detalhe));
	}

	private void invalidarTokens() {
		versaoCredencial++;
	}

	public Long getId() {
		return id;
	}

	public String getLogin() {
		return login;
	}

	public String getNome() {
		return nome;
	}

	public String getSenhaHash() {
		return senhaHash;
	}

	public Perfil getPerfil() {
		return perfil;
	}

	public boolean isAtivo() {
		return ativo;
	}

	public int getVersaoCredencial() {
		return versaoCredencial;
	}

	public OffsetDateTime getCriadoEm() {
		return criadoEm;
	}

	public OffsetDateTime getAtualizadoEm() {
		return atualizadoEm;
	}

	public List<HistoricoUsuario> getHistorico() {
		return Collections.unmodifiableList(historico);
	}
}
