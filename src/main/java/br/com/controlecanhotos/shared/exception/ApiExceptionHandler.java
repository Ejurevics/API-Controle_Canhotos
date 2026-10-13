package br.com.controlecanhotos.shared.exception;

import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

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
