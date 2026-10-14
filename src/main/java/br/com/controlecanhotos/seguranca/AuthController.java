package br.com.controlecanhotos.seguranca;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.controlecanhotos.seguranca.dto.LoginRequest;
import br.com.controlecanhotos.seguranca.dto.LoginResponse;
import br.com.controlecanhotos.usuario.UsuarioService;
import br.com.controlecanhotos.usuario.dto.TrocarSenhaRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

	private final AutenticacaoService service;
	private final UsuarioService usuarioService;

	public AuthController(AutenticacaoService service, UsuarioService usuarioService) {
		this.service = service;
		this.usuarioService = usuarioService;
	}

	@PostMapping("/login")
	@SecurityRequirements
	LoginResponse login(@RequestBody @Valid LoginRequest request, HttpServletRequest http) {
		return service.entrar(request.login(), request.senha(), http.getRemoteAddr());
	}

	/** Qualquer usuário logado troca a própria senha; o token atual continua valendo até expirar. */
	@PatchMapping("/senha")
	ResponseEntity<Void> trocarSenha(@RequestBody @Valid TrocarSenhaRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		usuarioService.trocarPropriaSenha(jwt.getSubject(), request.senhaAtual(), request.novaSenha());
		return ResponseEntity.noContent().build();
	}
}
