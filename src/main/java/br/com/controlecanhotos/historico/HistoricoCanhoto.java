package br.com.controlecanhotos.historico;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import br.com.controlecanhotos.canhoto.Canhoto;
import br.com.controlecanhotos.canhoto.StatusCanhoto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
@EntityListeners(AuditingEntityListener.class)
public class HistoricoCanhoto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "canhoto_id")
	private Canhoto canhoto;

	@Enumerated(EnumType.STRING)
	private StatusCanhoto statusAnterior;

	@Enumerated(EnumType.STRING)
	private StatusCanhoto statusNovo;

	private LocalDate dataRecebimento;

	private String observacao;

	private boolean dataFuturaConfirmada;

	@CreationTimestamp
	@Column(updatable = false)
	private OffsetDateTime registradoEm;

	/** Login de quem fez o registro; nulo nos registros anteriores à autenticação. */
	@CreatedBy
	@Column(updatable = false)
	private String registradoPor;

	protected HistoricoCanhoto() {
	}

	public HistoricoCanhoto(Canhoto canhoto, StatusCanhoto statusAnterior, StatusCanhoto statusNovo,
			LocalDate dataRecebimento, String observacao, boolean dataFuturaConfirmada) {
		this.canhoto = canhoto;
		this.statusAnterior = statusAnterior;
		this.statusNovo = statusNovo;
		this.dataRecebimento = dataRecebimento;
		this.observacao = observacao;
		this.dataFuturaConfirmada = dataFuturaConfirmada;
	}

	public Long getId() {
		return id;
	}

	public Canhoto getCanhoto() {
		return canhoto;
	}

	public StatusCanhoto getStatusAnterior() {
		return statusAnterior;
	}

	public StatusCanhoto getStatusNovo() {
		return statusNovo;
	}

	public LocalDate getDataRecebimento() {
		return dataRecebimento;
	}

	public String getObservacao() {
		return observacao;
	}

	public boolean isDataFuturaConfirmada() {
		return dataFuturaConfirmada;
	}

	public OffsetDateTime getRegistradoEm() {
		return registradoEm;
	}

	public String getRegistradoPor() {
		return registradoPor;
	}
}
