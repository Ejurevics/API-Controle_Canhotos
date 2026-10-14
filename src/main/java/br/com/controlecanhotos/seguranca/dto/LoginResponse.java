package br.com.controlecanhotos.seguranca.dto;

import java.time.Instant;

import br.com.controlecanhotos.usuario.Perfil;

/** {@code token} vai no cabeçalho {@code Authorization: Bearer <token>} das demais requisições. */
public record LoginResponse(String token, String tipo, Instant expiraEm, Perfil perfil) {
}
