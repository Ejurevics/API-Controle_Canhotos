package br.com.controlecanhotos.canhoto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import br.com.controlecanhotos.carga.Carga;
import br.com.controlecanhotos.carga.StatusCarga;
import br.com.controlecanhotos.shared.exception.ConfirmacaoNecessariaException;
import br.com.controlecanhotos.shared.exception.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
public class Canhoto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "carga_id")
	private Carga carga;

	@Enumerated(EnumType.STRING)
	private StatusCanhoto status;

	private LocalDate dataRecebimento;

	private String observacao;

	@CreationTimestamp
	@Column(updatable = false)
	private OffsetDateTime criadoEm;

	@UpdateTimestamp
	private OffsetDateTime atualizadoEm;

	protected Canhoto() {
	}

	public Canhoto(Carga carga) {
		this.carga = carga;
		this.status = StatusCanhoto.PENDENTE;
	}

	public void registrarRecebimento(LocalDate dataRecebimento, boolean dataFuturaConfirmada) {
		exigirCargaEntregue();
		exigirDataRecebimento(dataRecebimento);
		exigirConfirmacaoSeDataFutura(dataRecebimento, dataFuturaConfirmada);
		this.dataRecebimento = dataRecebimento;
		this.status = StatusCanhoto.RECEBIDO;
	}

	public void registrarDivergencia(LocalDate dataRecebimento, String observacao,
			boolean dataFuturaConfirmada) {
		exigirCargaEntregue();
		exigirDataRecebimento(dataRecebimento);
		if (observacao == null || observacao.isBlank()) {
			throw new RegraNegocioException("Observação é obrigatória para registrar divergência");
		}
		exigirConfirmacaoSeDataFutura(dataRecebimento, dataFuturaConfirmada);
		this.dataRecebimento = dataRecebimento;
		this.observacao = observacao;
		this.status = StatusCanhoto.DIVERGENTE;
	}

	private void exigirCargaEntregue() {
		if (carga.getStatus() != StatusCarga.ENTREGUE) {
			throw new RegraNegocioException(
					"Canhoto só pode ser registrado com a carga ENTREGUE (status atual: %s)"
							.formatted(carga.getStatus()));
		}
	}

	private void exigirDataRecebimento(LocalDate dataRecebimento) {
		if (dataRecebimento == null) {
			throw new RegraNegocioException("Data de recebimento é obrigatória");
		}
	}

	private void exigirConfirmacaoSeDataFutura(LocalDate dataRecebimento, boolean dataFuturaConfirmada) {
		if (dataRecebimento.isAfter(LocalDate.now()) && !dataFuturaConfirmada) {
			throw new ConfirmacaoNecessariaException(
					"Data de recebimento %s é posterior à data atual; confirme para finalizar"
							.formatted(dataRecebimento));
		}
	}

	public Long getId() {
		return id;
	}

	public Carga getCarga() {
		return carga;
	}

	public StatusCanhoto getStatus() {
		return status;
	}

	public LocalDate getDataRecebimento() {
		return dataRecebimento;
	}

	public String getObservacao() {
		return observacao;
	}

	public OffsetDateTime getCriadoEm() {
		return criadoEm;
	}

	public OffsetDateTime getAtualizadoEm() {
		return atualizadoEm;
	}
}
