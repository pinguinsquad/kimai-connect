package de.p10d.kimai;

import de.p10d.kimai.cli.RootCommand;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import picocli.CommandLine;

@SpringBootApplication
@ConfigurationPropertiesScan
public class Application {

    public static void main(String[] args) {
        if (args.length > 0 && "mcp".equals(args[0])) {
            runMcpServer();
            return;
        }
        System.exit(runCli(args));
    }

    /**
     * MCP-Modus: stdio-Server für KI-Clients. Properties als Kommandozeilen-
     * Args, damit sie das enabled=false der application.yaml übersteuern;
     * keep-alive, weil der stdio-Transport auf Daemon-Threads läuft.
     */
    private static void runMcpServer() {
        new SpringApplicationBuilder(Application.class)
            .run(
                "--spring.ai.mcp.server.enabled=true",
                "--spring.ai.mcp.server.stdio=true",
                "--spring.main.keep-alive=true");
    }

    private static int runCli(String[] args) {
        try (ConfigurableApplicationContext context = SpringApplication.run(Application.class, args)) {
            CommandLine commandLine = new CommandLine(
                context.getBean(RootCommand.class),
                context.getBean(CommandLine.IFactory.class));
            commandLine.setExecutionExceptionHandler((exception, cmd, parseResult) -> {
                cmd.getErr().println("❌ " + exception.getMessage());
                return 1;
            });
            return commandLine.execute(args);
        }
    }
}
