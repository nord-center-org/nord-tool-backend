package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Token do aparelho: entregue uma única vez ao app, que o guarda no armazenamento privado do celular. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppAparelhoTokenDto {
    private String txToken;
}
