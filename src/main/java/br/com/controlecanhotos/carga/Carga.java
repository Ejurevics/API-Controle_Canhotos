package br.com.controlecanhotos.carga;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import br.com.controlecanhotos.canhoto.Canhoto;
import br.com.controlecanhotos.coleta.Coleta;
import br.com.controlecanhotos.historico.HistoricoStatus;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;

@Entity
public class Carga {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "coleta_id")
	private Coleta coleta;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(length = 14)
	private String emitenteCnpj;

	private short serieNotaFiscal;

	private int numeroNotaFiscal;

	private BigDecimal pesoKg;

	private String cidadeDestino;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(length = 2)
	private String ufDestino;

	@Enumerated(EnumType.STRING)
	private StatusCarga status;

	@OneToOne(mappedBy = "carga", cascade = CascadeType.ALL, optional = false)
	private Canhoto canhoto;

	@OneToMany(mappedBy = "carga", cascade = CascadeType.ALL)
	@OrderBy("alteradoEm, id")
	private List<HistoricoStatus> historico = new ArrayList<>();

	@CreationTimestamp
	@Column(updatable = false)
	private OffsetDateTime criadoEm;

	@UpdateTimestamp
	private OffsetDateTime atualizadoEm;

	protected Carga() {
	}

	public Carga(Coleta coleta, String emitenteCnpj, short serieNotaFiscal, int numeroNotaFiscal,
			BigDecimal pesoKg, String cidadeDestino, String ufDestino) {
		this.coleta = coleta;
		this.emitenteCnpj = emitenteCnpj;
		this.serieNotaFiscal = serieNotaFiscal;
		this.numeroNotaFiscal = numeroNotaFiscal;
		this.pesoKg = pesoKg;
		this.cidadeDestino = cidadeDestino;
		this.ufDestino = ufDestino;
		this.status = StatusCarga.AGUARDANDO_COLETA;
		this.canhoto = new Canhoto(this);
		this.historico.add(new HistoricoStatus(this, null, status, "Carga registrada"));
	}

	public void alterarStatus(StatusCarga novoStatus, String motivo) {
		if (!status.podeIrPara(novoStatus)) {
			throw new RegraNegocioException(
					"Transição de status inválida: %s -> %s".formatted(status, novoStatus));
		}
		historico.add(new HistoricoStatus(this, status, novoStatus, motivo));
		status = novoStatus;
	}

	public Long getId() {
		return id;
	}

	public Coleta getColeta() {
		return coleta;
	}

	public String getEmitenteCnpj() {
		return emitenteCnpj;
	}

	public short getSerieNotaFiscal() {
		return serieNotaFiscal;
	}

	public int getNumeroNotaFiscal() {
		return numeroNotaFiscal;
	}

	public BigDecimal getPesoKg() {
		return pesoKg;
	}

	public String getCidadeDestino() {
		return cidadeDestino;
	}

	public String getUfDestino() {
		return ufDestino;
	}

	public StatusCarga getStatus() {
		return status;
	}

	public Canhoto getCanhoto() {
		return canhoto;
	}

	public List<HistoricoStatus> getHistorico() {
		return Collections.unmodifiableList(historico);
	}

	public OffsetDateTime getCriadoEm() {
		return criadoEm;
	}

	public OffsetDateTime getAtualizadoEm() {
		return atualizadoEm;
	}
}
