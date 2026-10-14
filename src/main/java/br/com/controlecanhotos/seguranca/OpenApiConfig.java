package br.com.controlecanhotos.seguranca;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/** Botão "Authorize" no Swagger: cole o token devolvido por {@code POST /auth/login}. */
@Configuration
@OpenAPIDefinition(info = @Info(title = "API Controle de Canhotos"),
		security = @SecurityRequirement(name = OpenApiConfig.ESQUEMA))
@SecurityScheme(name = OpenApiConfig.ESQUEMA, type = SecuritySchemeType.HTTP, scheme = "bearer",
		bearerFormat = "JWT")
public class OpenApiConfig {

	static final String ESQUEMA = "bearer";
}
