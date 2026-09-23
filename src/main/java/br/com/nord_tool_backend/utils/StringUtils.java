package br.com.nord_tool_backend.utils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import java.text.MessageFormat;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;


public class StringUtils {

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(DateTimeFormatter.ISO_LOCAL_DATE, DateTimeFormatter.ofPattern("dd/MM/yyyy"));

    public static String getMensagem(final String chaveMensagem, final Object... params) {
        ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.getDefault());
        return recuperarTexto(bundle, chaveMensagem, params);
    }

    private static String recuperarTexto(final ResourceBundle bundle, final String chaveMensagem, final Object params) {
        String mensagem = "";

        try {
            mensagem = bundle.getString(chaveMensagem);
        } catch (MissingResourceException var5) {
            return chaveMensagem;
        }

        return (new MessageFormat(mensagem)).format(params);
    }

    public static String normalizeStatusVistoria(String value) {
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[\\s_-]+", "");
    }

    public static String normalizeDiaSemana(String nmDiaSemana) {
        if (nmDiaSemana == null) return null;

        return Normalizer.normalize(nmDiaSemana, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replace("-feira", "")
                .replace("feira", "")
                .replaceAll("\\s+", "")
                .trim();
    }

    protected String getCellString(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) return null;

        String value = cell.toString().trim();
        return value.isEmpty() ? null : value;
    }

    protected LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;

        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    protected static String parseHours(String value) {
        if (value == null || value.isBlank()) return null;

        try {
            return LocalTime.parse(value.trim()).format(DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Horário inválido: " + value);
        }
    }

    protected boolean parseBoolean(Cell cell) {
        return Optional.ofNullable(cell)
                .map(Cell::toString)
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(c -> Set.of("1","sim","s","true","yes","y").contains(c))
                .isPresent();
    }

    public static LocalDate parseDataPermitida(String data) {
        if (data == null || data.isBlank()) {
            return null;
        }
        List<DateTimeFormatter> formatosPermitidos = List.of(
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy")
        );

        for (DateTimeFormatter formatter : formatosPermitidos) {
            try {
                return LocalDate.parse(data, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new IllegalArgumentException("Formato de data inválido. Use dd/MM/yyyy ou dd-MM-yyyy");
    }
}
