package br.com.controlecanhotos.canhoto;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/** Limites do anexo do canhoto (ADR 0006). */
@ConfigurationProperties("app.anexo")
public record PropriedadesAnexo(DataSize tamanhoMaximo) {
}
