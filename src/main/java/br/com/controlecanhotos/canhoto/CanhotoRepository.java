package br.com.controlecanhotos.canhoto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CanhotoRepository extends JpaRepository<Canhoto, Long>, JpaSpecificationExecutor<Canhoto> {

	@Override
	@EntityGraph(attributePaths = { "carga", "carga.coleta" })
	Page<Canhoto> findAll(Specification<Canhoto> spec, Pageable pageable);
}
