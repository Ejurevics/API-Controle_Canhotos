package br.com.controlecanhotos.historico;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;

import br.com.controlecanhotos.carga.Carga;
import br.com.controlecanhotos.carga.StatusCarga;
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
public class HistoricoStatus {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "carga_id")
	private Carga carga;

	@Enumerated(EnumType.STRING)
	private StatusCarga statusAnterior;

	@Enumerated(EnumType.STRING)
	private StatusCarga statusNovo;

	@CreationTimestamp
	@Column(updatable = false)
	private OffsetDateTime alteradoEm;

	private String motivo;

	protected HistoricoStatus() {
	}

	public HistoricoStatus(Carga carga, StatusCarga statusAnterior, StatusCarga statusNovo, String motivo) {
		this.carga = carga;
		this.statusAnterior = statusAnterior;
		this.statusNovo = statusNovo;
		this.motivo = motivo;
	}

	public Long getId() {
		return id;
	}

	public Carga getCarga() {
		return carga;
	}

	public StatusCarga getStatusAnterior() {
		return statusAnterior;
	}

	public StatusCarga getStatusNovo() {
		return statusNovo;
	}

	public OffsetDateTime getAlteradoEm() {
		return alteradoEm;
	}

	public String getMotivo() {
		return motivo;
	}
}
