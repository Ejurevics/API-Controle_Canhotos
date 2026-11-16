package br.com.controlecanhotos.canhoto;

import org.springframework.data.domain.Persistable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Transient;

/**
 * Bytes de um anexo, com a mesma chave do {@link AnexoCanhoto}. Separados dos dados para que listar os
 * anexos não leia os arquivos; só o download chega aqui (ADR 0006).
 */
@Entity
public class AnexoCanhotoConteudo implements Persistable<Long> {

	@Id
	private Long anexoId;

	private byte[] conteudo;

	/** A chave vem do anexo; sem isto, o save do Spring Data faria um SELECT antes de inserir. */
	@Transient
	private boolean novo = true;

	protected AnexoCanhotoConteudo() {
	}

	AnexoCanhotoConteudo(Long anexoId, byte[] conteudo) {
		this.anexoId = anexoId;
		this.conteudo = conteudo.clone();
	}

	@PostLoad
	void carregado() {
		this.novo = false;
	}

	@Override
	public Long getId() {
		return anexoId;
	}

	@Override
	public boolean isNew() {
		return novo;
	}

	public byte[] getConteudo() {
		return conteudo.clone();
	}
}
