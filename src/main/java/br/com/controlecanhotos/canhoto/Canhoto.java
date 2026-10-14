package br.com.controlecanhotos.canhoto;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import br.com.controlecanhotos.carga.StatusCarga;
import br.com.controlecanhotos.historico.HistoricoCanhoto;
import br.com.controlecanhotos.notafiscal.NotaFiscal;
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

	private static final ZoneId FUSO_REFERENCIA = ZoneId.of("America/Sao_Paulo");

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "nota_fiscal_id")
	private NotaFiscal notaFiscal;

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

	public Canhoto(NotaFiscal notaFiscal) {
		this.notaFiscal = notaFiscal;
		this.status = StatusCanhoto.PENDENTE;
		this.historico.add(new HistoricoCanhoto(this, null, status, null, null, false));
	}

	public void registrarRecebimento(LocalDate dataRecebimento, boolean dataFuturaConfirmada) {
		registrarRecebimento(dataRecebimento, dataFuturaConfirmada, Clock.systemUTC());
	}

	public void registrarRecebimento(LocalDate dataRecebimento, boolean dataFuturaConfirmada, Clock relogio) {
		exigirCargaEntregue();
		exigirDataRecebimento(dataRecebimento);
		boolean dataFutura = exigirConfirmacaoSeDataFutura(dataRecebimento, dataFuturaConfirmada, relogio);
		registrar(StatusCanhoto.RECEBIDO, dataRecebimento, null, dataFutura);
	}

	public void registrarDivergencia(LocalDate dataRecebimento, String observacao,
			boolean dataFuturaConfirmada) {
		registrarDivergencia(dataRecebimento, observacao, dataFuturaConfirmada, Clock.systemUTC());
	}

	public void registrarDivergencia(LocalDate dataRecebimento, String observacao,
			boolean dataFuturaConfirmada, Clock relogio) {
		exigirCargaEntregue();
		exigirDataRecebimento(dataRecebimento);
		if (observacao == null || observacao.isBlank()) {
			throw new RegraNegocioException("Observação é obrigatória para registrar divergência");
		}
		boolean dataFutura = exigirConfirmacaoSeDataFutura(dataRecebimento, dataFuturaConfirmada, relogio);
		registrar(StatusCanhoto.DIVERGENTE, dataRecebimento, observacao, dataFutura);
	}

	private void registrar(StatusCanhoto novoStatus, LocalDate dataRecebimento, String observacao,
			boolean dataFuturaConfirmada) {
		historico.add(new HistoricoCanhoto(this, status, novoStatus, dataRecebimento, observacao,
				dataFuturaConfirmada));
		this.status = novoStatus;
		this.dataRecebimento = dataRecebimento;
		this.observacao = observacao;
	}

	private void exigirCargaEntregue() {
		if (notaFiscal.getCarga().getStatus() != StatusCarga.ENTREGUE) {
			throw new RegraNegocioException(
					"Canhoto só pode ser registrado com a carga ENTREGUE (status atual: %s)"
							.formatted(notaFiscal.getCarga().getStatus()));
		}
	}

	private void exigirDataRecebimento(LocalDate dataRecebimento) {
		if (dataRecebimento == null) {
			throw new RegraNegocioException("Data de recebimento é obrigatória");
		}
	}

	private boolean exigirConfirmacaoSeDataFutura(LocalDate dataRecebimento, boolean dataFuturaConfirmada,
			Clock relogio) {
		LocalDate hoje = LocalDate.now(relogio.withZone(FUSO_REFERENCIA));
		boolean dataFutura = dataRecebimento.isAfter(hoje);
		if (dataFutura && !dataFuturaConfirmada) {
			throw new ConfirmacaoNecessariaException(
					"Data de recebimento %s é posterior à data atual; confirme para finalizar"
							.formatted(dataRecebimento));
		}
		return dataFutura;
	}

	public Long getId() {
		return id;
	}

	public NotaFiscal getNotaFiscal() {
		return notaFiscal;
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
