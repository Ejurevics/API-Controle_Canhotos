package br.com.controlecanhotos.usuario;

import java.net.URI;

import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.controlecanhotos.shared.paginacao.ParametrosListagem;
import br.com.controlecanhotos.usuario.dto.AlterarAtivoRequest;
import br.com.controlecanhotos.usuario.dto.AlterarPerfilRequest;
import br.com.controlecanhotos.usuario.dto.CriarUsuarioRequest;
import br.com.controlecanhotos.usuario.dto.RedefinirSenhaRequest;
import br.com.controlecanhotos.usuario.dto.UsuarioResponse;
import jakarta.validation.Valid;

/** Gestão de usuários; todas as rotas exigem perfil ADMIN (veja {@code SegurancaConfig}). */
@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

	private final UsuarioService service;

	public UsuarioController(UsuarioService service) {
		this.service = service;
	}

	@PostMapping
	ResponseEntity<UsuarioResponse> criar(@RequestBody @Valid CriarUsuarioRequest request) {
		UsuarioResponse usuario = service.criar(request);
		return ResponseEntity.created(URI.create("/usuarios/" + usuario.id())).body(usuario);
	}

	@GetMapping
	PagedModel<UsuarioResponse> listar(
			@RequestParam(required = false) Perfil perfil,
			@RequestParam(required = false) Boolean ativo,
			@RequestParam(defaultValue = "0") int pagina,
			@RequestParam(defaultValue = "" + ParametrosListagem.TAMANHO_PADRAO) int tamanho) {
		return service.listar(perfil, ativo, pagina, tamanho);
	}

	@GetMapping("/{id}")
	UsuarioResponse buscar(@PathVariable Long id) {
		return service.buscar(id);
	}

	@PatchMapping("/{id}/ativo")
	UsuarioResponse alterarAtivo(@PathVariable Long id, @RequestBody @Valid AlterarAtivoRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return service.alterarAtivo(id, request.ativo(), jwt.getSubject());
	}

	@PatchMapping("/{id}/perfil")
	UsuarioResponse alterarPerfil(@PathVariable Long id, @RequestBody @Valid AlterarPerfilRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return service.alterarPerfil(id, request.perfil(), jwt.getSubject());
	}

	@PatchMapping("/{id}/senha")
	ResponseEntity<Void> redefinirSenha(@PathVariable Long id, @RequestBody @Valid RedefinirSenhaRequest request) {
		service.redefinirSenha(id, request.novaSenha());
		return ResponseEntity.noContent().build();
	}
}
