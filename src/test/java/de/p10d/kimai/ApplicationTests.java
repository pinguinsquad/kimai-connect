package de.p10d.kimai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ApplicationTests {

    @Value("${app.version}")
    String appVersion;

    @Value("${spring.ai.mcp.server.version}")
    String mcpServerVersion;

    @Test
    void contextLoads() {
    }

    @Test
    void versionKommtAusDemMavenBuild() {
        // Maven-Resource-Filtering ersetzt @project.version@ (eine Quelle für CLI und MCP)
        assertThat(appVersion).isNotBlank().doesNotContain("@");
        assertThat(mcpServerVersion).isEqualTo(appVersion);
    }
}
