package de.p10d.kimai.cli;

import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Maschinenlesbare JSON-Ausgabe (Spec 001 FA-6, Spec 002 FA-3);
 * eine Zeile, Schema gemäß Plan 001, Felder ohne Wert explizit als null.
 * Gemeinsame Serialisierung für CLI und MCP-Tools.
 */
@Component
public class JsonRenderer {

    private final ObjectMapper mapper = JsonMapper.builder().build();

    public String render(Object value) {
        return mapper.writeValueAsString(value);
    }
}
