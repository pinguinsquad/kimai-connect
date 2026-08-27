package de.p10d.kimai.cli;

import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

/**
 * Stub für die Hilfe: Der eigentliche MCP-Modus wird in Application.main
 * erkannt, bevor picocli läuft (Plan 002 E-4). Dieses Kommando wird nur
 * erreicht, wenn "mcp" nicht das erste Argument ist.
 */
@Component
@Command(
    name = "mcp",
    description = "Startet den MCP-Server (stdio) für KI-Clients, z. B. Claude Code.",
    synopsisHeading = "Aufruf: ",
    optionListHeading = "Optionen:%n")
public class McpCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Override
    public Integer call() {
        spec.commandLine().getErr()
            .println("❌ \"mcp\" muss das erste Argument sein.");
        return 2;
    }
}
