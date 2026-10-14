package br.com.controlecanhotos.usuario;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String login;

	private String nome;

	private String senhaHash;

	@Enumerated(EnumType.STRING)
	private Perfil perfil;

	private boolean ativo;

	@CreationTimestamp
	@Column(updatable = false)
	private OffsetDateTime criadoEm;

	@UpdateTimestamp
	private OffsetDateTime atualizadoEm;

	protected Usuario() {
	}

	/** {@code senhaHash} já vem codificada (BCrypt); a entidade nunca vê a senha em texto. */
	public Usuario(String login, String nome, String senhaHash, Perfil perfil) {
		this.login = normalizarLogin(login);
		this.nome = nome;
		this.senhaHash = senhaHash;
		this.perfil = perfil;
		this.ativo = true;
	}

	/** Login é guardado e comparado sem espaços nas pontas e em minúsculas. */
	public static String normalizarLogin(String login) {
		return login == null ? null : login.strip().toLowerCase();
	}

	public void desativar() {
		ativo = false;
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

	public OffsetDateTime getCriadoEm() {
		return criadoEm;
	}

	public OffsetDateTime getAtualizadoEm() {
		return atualizadoEm;
	}
}
