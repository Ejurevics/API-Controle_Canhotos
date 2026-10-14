package br.com.controlecanhotos.usuario;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

	Optional<Usuario> findByLogin(String login);

	boolean existsByLogin(String login);

	boolean existsByPerfil(Perfil perfil);

	long countByPerfilAndAtivoTrue(Perfil perfil);
}
