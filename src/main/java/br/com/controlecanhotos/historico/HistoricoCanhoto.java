package br.com.controlecanhotos.historico;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;

import br.com.controlecanhotos.canhoto.Canhoto;
import br.com.controlecanhotos.canhoto.StatusCanhoto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
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

	@CreationTimestamp
	@Column(updatable = false)
	private OffsetDateTime registradoEm;

	protected HistoricoCanhoto() {
	}

	public HistoricoCanhoto(Canhoto canhoto, StatusCanhoto statusAnterior, StatusCanhoto statusNovo,
			LocalDate dataRecebimento, String observacao) {
		this.canhoto = canhoto;
		this.statusAnterior = statusAnterior;
		this.statusNovo = statusNovo;
		this.dataRecebimento = dataRecebimento;
		this.observacao = observacao;
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

	public OffsetDateTime getRegistradoEm() {
		return registradoEm;
	}
}
