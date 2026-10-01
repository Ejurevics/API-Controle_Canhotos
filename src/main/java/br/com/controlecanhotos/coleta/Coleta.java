package br.com.controlecanhotos.coleta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import br.com.controlecanhotos.carga.Carga;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

@Entity
public class Coleta {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String clienteNome;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(length = 14)
	private String clienteCnpj;

	private LocalDate dataProgramada;

	@Enumerated(EnumType.STRING)
	private SituacaoColeta situacao;

	@OneToMany(mappedBy = "coleta", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id")
	private List<Carga> cargas = new ArrayList<>();

	@CreationTimestamp
	@Column(updatable = false)
	private OffsetDateTime criadoEm;

	@UpdateTimestamp
	private OffsetDateTime atualizadoEm;

	protected Coleta() {
	}

	public Coleta(String clienteNome, String clienteCnpj, LocalDate dataProgramada) {
		this.clienteNome = clienteNome;
		this.clienteCnpj = clienteCnpj;
		this.dataProgramada = dataProgramada;
		this.situacao = SituacaoColeta.PROGRAMADA;
	}

	public Carga adicionarCarga(String emitenteCnpj, short serieNotaFiscal, int numeroNotaFiscal,
			BigDecimal pesoKg, String cidadeDestino, String ufDestino) {
		Carga carga = new Carga(this, emitenteCnpj, serieNotaFiscal, numeroNotaFiscal,
				pesoKg, cidadeDestino, ufDestino);
		cargas.add(carga);
		return carga;
	}

	public Long getId() {
		return id;
	}

	public String getClienteNome() {
		return clienteNome;
	}

	public String getClienteCnpj() {
		return clienteCnpj;
	}

	public LocalDate getDataProgramada() {
		return dataProgramada;
	}

	public SituacaoColeta getSituacao() {
		return situacao;
	}

	public List<Carga> getCargas() {
		return Collections.unmodifiableList(cargas);
	}

	public OffsetDateTime getCriadoEm() {
		return criadoEm;
	}

	public OffsetDateTime getAtualizadoEm() {
		return atualizadoEm;
	}
}
