package br.com.nord_tool_backend.service.marketing;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/** Números da conta e os posts mais recentes. */
@Getter
@AllArgsConstructor
public class PainelRede {
    private final Long seguidores;
    private final Long viewsTotais;
    private final List<PostRede> posts;
}
