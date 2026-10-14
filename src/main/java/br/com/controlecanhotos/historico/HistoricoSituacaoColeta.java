package br.com.controlecanhotos.historico;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import br.com.controlecanhotos.coleta.Coleta;
import br.com.controlecanhotos.coleta.SituacaoColeta;
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
public class HistoricoSituacaoColeta {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "coleta_id")
	private Coleta coleta;

	@Enumerated(EnumType.STRING)
	private SituacaoColeta situacaoAnterior;

	@Enumerated(EnumType.STRING)
	private SituacaoColeta situacaoNova;

	@CreationTimestamp
	@Column(updatable = false)
	private OffsetDateTime alteradoEm;

	/** Login de quem fez a mudança; nulo nos registros criados pela migração. */
	@CreatedBy
	@Column(updatable = false)
	private String alteradoPor;

	protected HistoricoSituacaoColeta() {
	}

	public HistoricoSituacaoColeta(Coleta coleta, SituacaoColeta situacaoAnterior, SituacaoColeta situacaoNova) {
		this.coleta = coleta;
		this.situacaoAnterior = situacaoAnterior;
		this.situacaoNova = situacaoNova;
	}

	public Long getId() {
		return id;
	}

	public Coleta getColeta() {
		return coleta;
	}

	public SituacaoColeta getSituacaoAnterior() {
		return situacaoAnterior;
	}

	public SituacaoColeta getSituacaoNova() {
		return situacaoNova;
	}

	public OffsetDateTime getAlteradoEm() {
		return alteradoEm;
	}

	public String getAlteradoPor() {
		return alteradoPor;
	}
}
