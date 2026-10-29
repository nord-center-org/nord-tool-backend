package br.com.nord_tool_backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Grade da torre depois das mudanças (todas as células, já com as de outras pessoas). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EscadinhaGradeDto {
    private JsonNode grade;
}
