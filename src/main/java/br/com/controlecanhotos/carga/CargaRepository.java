package br.com.controlecanhotos.carga;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CargaRepository extends JpaRepository<Carga, Long>, JpaSpecificationExecutor<Carga> {

	boolean existsByEmitenteCnpjAndSerieNotaFiscalAndNumeroNotaFiscal(String emitenteCnpj,
			short serieNotaFiscal, int numeroNotaFiscal);

	@Override
	@EntityGraph(attributePaths = "canhoto")
	Page<Carga> findAll(Specification<Carga> spec, Pageable pageable);
}
