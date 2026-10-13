package br.com.controlecanhotos.carga;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CargaRepository extends JpaRepository<Carga, Long> {

	boolean existsByEmitenteCnpjAndSerieNotaFiscalAndNumeroNotaFiscal(String emitenteCnpj,
			short serieNotaFiscal, int numeroNotaFiscal);
}
