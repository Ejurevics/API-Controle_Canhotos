package br.com.controlecanhotos.carga;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CargaRepository extends JpaRepository<Carga, Long>, JpaSpecificationExecutor<Carga> {

	boolean existsByChaveAcesso(String chaveAcesso);
}
