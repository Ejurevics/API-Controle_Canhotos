package br.com.controlecanhotos.coleta;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import br.com.controlecanhotos.carga.Carga;
import br.com.controlecanhotos.historico.HistoricoSituacaoColeta;
import br.com.controlecanhotos.shared.exception.RegraNegocioException;
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

	@OneToMany(mappedBy = "coleta", cascade = CascadeType.ALL)
	@OrderBy("alteradoEm, id")
	private List<HistoricoSituacaoColeta> historico = new ArrayList<>();

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
		this.historico.add(new HistoricoSituacaoColeta(this, null, situacao));
	}

	public Carga adicionarCarga(String chaveAcesso, String cidadeDestino, String ufDestino) {
		Carga carga = new Carga(this, chaveAcesso, cidadeDestino, ufDestino);
		cargas.add(carga);
		return carga;
	}

	public void alterarSituacao(SituacaoColeta novaSituacao) {
		if (!situacao.podeIrPara(novaSituacao)) {
			throw new RegraNegocioException(
					"Transição de situação inválida: %s -> %s".formatted(situacao, novaSituacao));
		}
		if (novaSituacao == SituacaoColeta.CONCLUIDA) {
			exigirCargasFinalizadas();
		}
		historico.add(new HistoricoSituacaoColeta(this, situacao, novaSituacao));
		situacao = novaSituacao;
	}

	private void exigirCargasFinalizadas() {
		String pendentes = cargas.stream()
				.filter(carga -> !carga.getStatus().finalizada())
				.map(carga -> "%s (%s)".formatted(carga.getChaveAcesso(), carga.getStatus()))
				.collect(Collectors.joining(", "));
		if (!pendentes.isEmpty()) {
			throw new RegraNegocioException(
					"Coleta só pode ser concluída com todas as cargas ENTREGUE ou DEVOLVIDA; pendentes (CT-e): "
							+ pendentes);
		}
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

	public List<HistoricoSituacaoColeta> getHistorico() {
		return Collections.unmodifiableList(historico);
	}

	public OffsetDateTime getCriadoEm() {
		return criadoEm;
	}

	public OffsetDateTime getAtualizadoEm() {
		return atualizadoEm;
	}
}
