package br.com.controlecanhotos.seguranca;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.controlecanhotos.seguranca.dto.LoginRequest;
import br.com.controlecanhotos.seguranca.dto.LoginResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

	private final AutenticacaoService service;

	public AuthController(AutenticacaoService service) {
		this.service = service;
	}

	@PostMapping("/login")
	@SecurityRequirements
	LoginResponse login(@RequestBody @Valid LoginRequest request) {
		return service.entrar(request.login(), request.senha());
	}
}
