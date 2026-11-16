package br.com.controlecanhotos.canhoto;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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

/**
 * Dados de um comprovante anexado ao canhoto (ADR 0006). Os bytes ficam em {@link AnexoCanhotoConteudo},
 * para listar os anexos sem ler os arquivos.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
public class AnexoCanhoto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "canhoto_id")
	private Canhoto canhoto;

	private String nomeArquivo;

	@Enumerated(EnumType.STRING)
	private TipoAnexo tipo;

	private int tamanhoBytes;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(length = 64)
	private String sha256;

	@CreationTimestamp
	@Column(updatable = false)
	private OffsetDateTime enviadoEm;

	@CreatedBy
	@Column(updatable = false)
	private String enviadoPor;

	protected AnexoCanhoto() {
	}

	AnexoCanhoto(Canhoto canhoto, String nomeArquivo, TipoAnexo tipo, int tamanhoBytes, String sha256) {
		this.canhoto = canhoto;
		this.nomeArquivo = nomeArquivo;
		this.tipo = tipo;
		this.tamanhoBytes = tamanhoBytes;
		this.sha256 = sha256;
	}

	public Long getId() {
		return id;
	}

	public Canhoto getCanhoto() {
		return canhoto;
	}

	public String getNomeArquivo() {
		return nomeArquivo;
	}

	public TipoAnexo getTipo() {
		return tipo;
	}

	public int getTamanhoBytes() {
		return tamanhoBytes;
	}

	public String getSha256() {
		return sha256;
	}

	public OffsetDateTime getEnviadoEm() {
		return enviadoEm;
	}

	public String getEnviadoPor() {
		return enviadoPor;
	}
}
