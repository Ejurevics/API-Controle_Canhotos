package br.com.controlecanhotos.historico;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import br.com.controlecanhotos.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
@EntityListeners(AuditingEntityListener.class)
public class HistoricoUsuario {

	public enum Acao {
		CRIADO,
		DESATIVADO,
		REATIVADO,
		PERFIL_ALTERADO,
		/** Pelo ADMIN, em {@code PATCH /usuarios/{id}/senha}. */
		SENHA_REDEFINIDA,
		/** Pelo próprio usuário, em {@code PATCH /auth/senha}. */
		SENHA_TROCADA
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id")
	private Usuario usuario;

	@Enumerated(EnumType.STRING)
	private Acao acao;

	/** Só na troca de perfil: {@code OPERADOR → ADMIN}. */
	private String detalhe;

	@CreationTimestamp
	@Column(updatable = false)
	private OffsetDateTime alteradoEm;

	/** Login de quem fez; nulo no ADMIN inicial (criado na subida) e nos registros criados pela migração. */
	@CreatedBy
	@Column(updatable = false)
	private String alteradoPor;

	protected HistoricoUsuario() {
	}

	public HistoricoUsuario(Usuario usuario, Acao acao, String detalhe) {
		this.usuario = usuario;
		this.acao = acao;
		this.detalhe = detalhe;
	}

	public Long getId() {
		return id;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public Acao getAcao() {
		return acao;
	}

	public String getDetalhe() {
		return detalhe;
	}

	public OffsetDateTime getAlteradoEm() {
		return alteradoEm;
	}

	public String getAlteradoPor() {
		return alteradoPor;
	}
}
