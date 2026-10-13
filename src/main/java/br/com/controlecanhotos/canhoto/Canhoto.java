package br.com.controlecanhotos.canhoto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import br.com.controlecanhotos.carga.Carga;
import br.com.controlecanhotos.carga.StatusCarga;
import br.com.controlecanhotos.historico.HistoricoCanhoto;
import br.com.controlecanhotos.shared.exception.ConfirmacaoNecessariaException;
import br.com.controlecanhotos.shared.exception.RegraNegocioException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;

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

	@OneToMany(mappedBy = "canhoto", cascade = CascadeType.ALL)
	@OrderBy("registradoEm, id")
	private List<HistoricoCanhoto> historico = new ArrayList<>();

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
		this.historico.add(new HistoricoCanhoto(this, null, status, null, null));
	}

	public void registrarRecebimento(LocalDate dataRecebimento, boolean dataFuturaConfirmada) {
		exigirCargaEntregue();
		exigirDataRecebimento(dataRecebimento);
		exigirConfirmacaoSeDataFutura(dataRecebimento, dataFuturaConfirmada);
		registrar(StatusCanhoto.RECEBIDO, dataRecebimento, null);
	}

	public void registrarDivergencia(LocalDate dataRecebimento, String observacao,
			boolean dataFuturaConfirmada) {
		exigirCargaEntregue();
		exigirDataRecebimento(dataRecebimento);
		if (observacao == null || observacao.isBlank()) {
			throw new RegraNegocioException("Observação é obrigatória para registrar divergência");
		}
		exigirConfirmacaoSeDataFutura(dataRecebimento, dataFuturaConfirmada);
		registrar(StatusCanhoto.DIVERGENTE, dataRecebimento, observacao);
	}

	private void registrar(StatusCanhoto novoStatus, LocalDate dataRecebimento, String observacao) {
		historico.add(new HistoricoCanhoto(this, status, novoStatus, dataRecebimento, observacao));
		this.status = novoStatus;
		this.dataRecebimento = dataRecebimento;
		this.observacao = observacao;
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

	public List<HistoricoCanhoto> getHistorico() {
		return Collections.unmodifiableList(historico);
	}

	public OffsetDateTime getCriadoEm() {
		return criadoEm;
	}

	public OffsetDateTime getAtualizadoEm() {
		return atualizadoEm;
	}
}
