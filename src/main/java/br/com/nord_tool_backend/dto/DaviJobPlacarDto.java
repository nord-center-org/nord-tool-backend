package br.com.nord_tool_backend.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** Ranking (top 10) e o recorde de quem está jogando. */
@Data
public class DaviJobPlacarDto {
    private List<DaviJobRankingDto> ranking = new ArrayList<>();
    /** Melhor pontuação do usuário logado (null se nunca jogou). */
    private DaviJobRankingDto meuRecorde;
}
