package br.com.controlecanhotos.carga;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import br.com.controlecanhotos.coleta.Coleta;
import br.com.controlecanhotos.historico.HistoricoStatus;
import br.com.controlecanhotos.notafiscal.NotaFiscal;
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
import jakarta.persistence.OrderBy;

/**
 * Carga transportada, identificada pelo CT-e (chave de acesso de 44 dígitos). Agrupa de 1 a N notas fiscais.
 */
@Entity
public class Carga {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "coleta_id")
	private Coleta coleta;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(length = 44)
	private String chaveAcesso;

	private boolean chaveProvisoria;

	private String cidadeDestino;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(length = 2)
	private String ufDestino;

	@Enumerated(EnumType.STRING)
	private StatusCarga status;

	@OneToMany(mappedBy = "carga", cascade = CascadeType.ALL)
	@OrderBy("id")
	private List<NotaFiscal> notasFiscais = new ArrayList<>();

	@OneToMany(mappedBy = "carga", cascade = CascadeType.ALL)
	@OrderBy("alteradoEm, id")
	private List<HistoricoStatus> historico = new ArrayList<>();

	/** Somente leitura: calculado pelo banco para as listagens não carregarem as notas de cada carga. */
	@Formula("(select count(*) from nota_fiscal nf where nf.carga_id = id)")
	private int quantidadeNotasFiscais;

	/** Somente leitura: calculado pelo banco, como {@link #quantidadeNotasFiscais}. */
	@Formula("(select count(*) from nota_fiscal nf join canhoto c on c.nota_fiscal_id = nf.id "
			+ "where nf.carga_id = id and c.status = 'PENDENTE')")
	private int canhotosPendentes;

	@CreationTimestamp
	@Column(updatable = false)
	private OffsetDateTime criadoEm;

	@UpdateTimestamp
	private OffsetDateTime atualizadoEm;

	protected Carga() {
	}

	public Carga(Coleta coleta, String chaveAcesso, String cidadeDestino, String ufDestino) {
		this.coleta = coleta;
		this.chaveAcesso = chaveAcesso;
		this.cidadeDestino = cidadeDestino;
		this.ufDestino = ufDestino;
		this.status = StatusCarga.AGUARDANDO_COLETA;
		this.historico.add(new HistoricoStatus(this, null, status, "Carga registrada"));
	}

	public NotaFiscal adicionarNotaFiscal(String emitenteCnpj, short serieNotaFiscal, int numeroNotaFiscal,
			BigDecimal pesoKg) {
		boolean repetida = notasFiscais.stream()
				.anyMatch(nota -> nota.corresponde(emitenteCnpj, serieNotaFiscal, numeroNotaFiscal));
		if (repetida) {
			throw new RegraNegocioException("Nota fiscal repetida no CT-e %s: %s/%d/%d".formatted(
					chaveAcesso, emitenteCnpj, serieNotaFiscal, numeroNotaFiscal));
		}
		NotaFiscal nota = new NotaFiscal(this, emitenteCnpj, serieNotaFiscal, numeroNotaFiscal, pesoKg);
		notasFiscais.add(nota);
		return nota;
	}

	public void alterarStatus(StatusCarga novoStatus, String motivo) {
		if (!status.podeIrPara(novoStatus)) {
			throw new RegraNegocioException(
					"Transição de status inválida: %s -> %s".formatted(status, novoStatus));
		}
		if (novoStatus == StatusCarga.OCORRENCIA && (motivo == null || motivo.isBlank())) {
			throw new RegraNegocioException("Motivo é obrigatório para registrar ocorrência");
		}
		historico.add(new HistoricoStatus(this, status, novoStatus, motivo));
		status = novoStatus;
	}

	public BigDecimal getPesoTotalKg() {
		return notasFiscais.stream().map(NotaFiscal::getPesoKg).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public Long getId() {
		return id;
	}

	public Coleta getColeta() {
		return coleta;
	}

	public String getChaveAcesso() {
		return chaveAcesso;
	}

	public boolean isChaveProvisoria() {
		return chaveProvisoria;
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

	public List<NotaFiscal> getNotasFiscais() {
		return Collections.unmodifiableList(notasFiscais);
	}

	public List<HistoricoStatus> getHistorico() {
		return Collections.unmodifiableList(historico);
	}

	/** Só tem valor em cargas lidas do banco; use {@code getNotasFiscais().size()} em cargas em memória. */
	public int getQuantidadeNotasFiscais() {
		return quantidadeNotasFiscais;
	}

	/** Só tem valor em cargas lidas do banco, como {@link #getQuantidadeNotasFiscais()}. */
	public int getCanhotosPendentes() {
		return canhotosPendentes;
	}

	public OffsetDateTime getCriadoEm() {
		return criadoEm;
	}

	public OffsetDateTime getAtualizadoEm() {
		return atualizadoEm;
	}
}
