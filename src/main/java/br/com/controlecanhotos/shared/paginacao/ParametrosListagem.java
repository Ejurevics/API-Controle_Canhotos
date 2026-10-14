package br.com.controlecanhotos.shared.paginacao;

import java.time.LocalDate;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import br.com.controlecanhotos.shared.exception.RegraNegocioException;

public final class ParametrosListagem {

	public static final int TAMANHO_PADRAO = 20;
	public static final int TAMANHO_MAXIMO = 100;

	private ParametrosListagem() {
	}

	/** A ordenação é sempre fixa por endpoint; o cliente não escolhe o campo (evita erro por campo inexistente). */
	public static Pageable paginar(int pagina, int tamanho, Sort ordenacao) {
		if (pagina < 0) {
			throw new RegraNegocioException("Página não pode ser negativa");
		}
		if (tamanho < 1 || tamanho > TAMANHO_MAXIMO) {
			throw new RegraNegocioException("Tamanho da página deve estar entre 1 e " + TAMANHO_MAXIMO);
		}
		return PageRequest.of(pagina, tamanho, ordenacao);
	}

	public static void validarPeriodo(LocalDate dataInicio, LocalDate dataFim) {
		if (dataInicio != null && dataFim != null && dataInicio.isAfter(dataFim)) {
			throw new RegraNegocioException("dataInicio não pode ser posterior a dataFim");
		}
	}
}
