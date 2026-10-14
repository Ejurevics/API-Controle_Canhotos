package br.com.controlecanhotos.carga.dto.validacao;

import br.com.controlecanhotos.carga.ChaveAcessoCte;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ChaveAcessoCteValidator implements ConstraintValidator<ChaveAcessoCteValida, String> {

	@Override
	public boolean isValid(String chave, ConstraintValidatorContext context) {
		if (chave == null) {
			return true;
		}
		return ChaveAcessoCte.problema(chave)
				.map(problema -> {
					context.disableDefaultConstraintViolation();
					context.buildConstraintViolationWithTemplate("Chave de acesso de CT-e " + problema)
							.addConstraintViolation();
					return false;
				})
				.orElse(true);
	}
}
