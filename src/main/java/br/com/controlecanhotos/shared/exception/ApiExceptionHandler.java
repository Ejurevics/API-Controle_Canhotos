package br.com.controlecanhotos.shared.exception;

import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(AuthenticationException.class)
	ResponseEntity<ProblemDetail> naoAutenticado(AuthenticationException ex) {
		String detalhe = ex instanceof BadCredentialsException ? ex.getMessage()
				: "Autenticação necessária: envie um token válido em Authorization: Bearer <token>";
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
				.body(ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detalhe));
	}

	@ExceptionHandler(AccessDeniedException.class)
	ProblemDetail semPermissao(AccessDeniedException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
				"Seu perfil não tem permissão para esta operação");
	}

	@ExceptionHandler(RecursoNaoEncontradoException.class)
	ProblemDetail recursoNaoEncontrado(RecursoNaoEncontradoException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(ConfirmacaoNecessariaException.class)
	ProblemDetail confirmacaoNecessaria(ConfirmacaoNecessariaException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problem.setProperty("confirmacaoNecessaria", true);
		return problem;
	}

	@ExceptionHandler(RegraNegocioException.class)
	ProblemDetail regraNegocio(RegraNegocioException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail integridade(DataIntegrityViolationException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"Operação viola uma restrição de integridade dos dados");
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail validacao(MethodArgumentNotValidException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"Dados inválidos na requisição");
		problem.setProperty("erros", ex.getBindingResult().getFieldErrors().stream()
				.collect(Collectors.toMap(e -> e.getField(),
						e -> e.getDefaultMessage() == null ? "inválido" : e.getDefaultMessage(),
						(a, b) -> a)));
		return problem;
	}
}
