package br.com.controlecanhotos.notafiscal;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import br.com.controlecanhotos.canhoto.Canhoto;
import br.com.controlecanhotos.carga.Carga;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

@Entity
public class NotaFiscal {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "carga_id")
	private Carga carga;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(length = 14)
	private String emitenteCnpj;

	private short serieNotaFiscal;

	private int numeroNotaFiscal;

	private BigDecimal pesoKg;

	@OneToOne(mappedBy = "notaFiscal", cascade = CascadeType.ALL, optional = false)
	private Canhoto canhoto;

	@CreationTimestamp
	@Column(updatable = false)
	private OffsetDateTime criadoEm;

	@UpdateTimestamp
	private OffsetDateTime atualizadoEm;

	protected NotaFiscal() {
	}

	public NotaFiscal(Carga carga, String emitenteCnpj, short serieNotaFiscal, int numeroNotaFiscal,
			BigDecimal pesoKg) {
		this.carga = carga;
		this.emitenteCnpj = emitenteCnpj;
		this.serieNotaFiscal = serieNotaFiscal;
		this.numeroNotaFiscal = numeroNotaFiscal;
		this.pesoKg = pesoKg;
		this.canhoto = new Canhoto(this);
	}

	public boolean corresponde(String emitenteCnpj, short serieNotaFiscal, int numeroNotaFiscal) {
		return this.emitenteCnpj.equals(emitenteCnpj) && this.serieNotaFiscal == serieNotaFiscal
				&& this.numeroNotaFiscal == numeroNotaFiscal;
	}

	public Long getId() {
		return id;
	}

	public Carga getCarga() {
		return carga;
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

	public Canhoto getCanhoto() {
		return canhoto;
	}

	public OffsetDateTime getCriadoEm() {
		return criadoEm;
	}

	public OffsetDateTime getAtualizadoEm() {
		return atualizadoEm;
	}
}
