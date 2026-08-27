package de.p10d.kimai.cli;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

/**
 * Liefert Anwendungsname und Version aus dem Maven-Build (app.name/
 * app.version via Resource-Filtering) — eine Quelle für CLI und MCP-Server
 * (Plan 002 B-5); nutzbar von aufbauenden Projekten.
 */
@Component
public class VersionProvider implements CommandLine.IVersionProvider {

    private final String name;
    private final String version;

    @Autowired
    public VersionProvider(@Value("${app.name}") String name, @Value("${app.version}") String version) {
        this.name = name;
        this.version = version;
    }

    /** Für picocli ohne Spring-Kontext (Unit-Tests): Reflection braucht no-arg. */
    public VersionProvider() {
        this("kimai-connect", "dev");
    }

    @Override
    public String[] getVersion() {
        return new String[]{name + " " + version};
    }
}
