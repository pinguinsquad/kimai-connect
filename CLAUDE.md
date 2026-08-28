# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Sprache und Konventionen

Dokumentation, Commit-Messages, Kommentare und Nutzer-Meldungen auf Deutsch; Code-Bezeichner
auf Englisch. Commit-Betreff kurz im Präsens („README ergänzt“). Details in CONTRIBUTING.md.

## Build und Tests

Java 25 und Maven 3.9 (per asdf aus `.tool-versions`). Der Enforcer bricht bei älteren
Versionen und bei Dependency-Konvergenz-Verstößen ab.

```bash
mvn verify                                   # Build + alle Tests (= CI)
mvn test -Dtest=TimesheetServiceTest         # eine Testklasse
mvn test -Dtest=KimaiClientTest#paginiertBisAlleEintraegeGeladenSind  # eine Testmethode
```

Ergebnis: `target/kimai-connect-<version>.jar` (Bibliothek) und `…-exec.jar` (lauffähig,
Spring-Boot-Classifier `exec`).

Konfiguration ausschließlich über die Umgebungsvariablen `KIMAI_BASE_URL` und `KIMAI_API_TOKEN`.
Wie das Token bereitgestellt wird, entscheidet der Anwender – der Aufruf mit plain Env-Var muss
immer funktionieren, auch in MCP-Client-Konfigurationen; die 1Password-CLI (`op run`) ist nur
eine Option und darf in Doku und Beispielen nicht als Voraussetzung erscheinen.

```bash
KIMAI_BASE_URL=https://kimai.example.org/api KIMAI_API_TOKEN=<token> java -jar target/kimai-connect-<version>-exec.jar list --start 2026-08-01 --end 2026-08-31
```

Test-Instanz in diesem Projekt: `https://tr.p10d.de/api`, Token in 1Password unter
`op://Hosting/Kimai Projekt/API Token` (Aufruf dann mit `op run --no-masking -- java …`).

## Release-Modell

Es gibt nur `main`; **jeder Merge auf `main` ist ein Release**. Kein `develop`, keine
`-SNAPSHOT`-Versionen. Jeder PR hebt die Version in `pom.xml` (plus
`project.build.outputTimestamp`) und legt einen CHANGELOG-Abschnitt an; `release.yml` setzt
nach dem Merge Tag, GitHub-Release und Deploy nach GitHub Packages automatisch und bricht ab,
wenn der Tag schon existiert. Dependabot-PRs bekommen den Patch-Bump vor dem Merge nachgezogen.
Nie direkt auf `main` committen. Ablauf in RELEASING.md.

## Architektur

Spring-Boot-Anwendung ohne Web (`web-application-type: none`), die drei Oberflächen über
einen gemeinsamen Kern bedient. Das Jar ist zugleich Bibliothek für
[kimai2lexware-v2](https://github.com/pinguinsquad/dies-und-das/tree/main/kimai2lexware-v2);
öffentliche Klassen in `core`, `client`, `pdf`, `mcp` (z. B. `McpToolSupport`) sind daher API.

Pakete unter `de.p10d.kimai`:

- `core` – fachliches Modell (Records `TimesheetQuery`, `TimesheetEntry`, `TimesheetReport`,
  `UserInfo`), `TimesheetService` als Einstiegspunkt und die vom Kern definierten Ports
  `TimesheetSource` / `UserSource`. Keine Abhängigkeit zu Kimai oder Spring-Web.
- `client` – Kimai-REST-Adapter: `KimaiClient` implementiert beide Ports (RestClient,
  paginiert mit `kimai.page-size`), `KimaiProperties` bindet `KIMAI_BASE_URL`/`KIMAI_API_TOKEN`,
  `KimaiException` für Fehler mit deutscher Meldung. Alles nur lesend.
- `cli` – picocli-Kommandos `list`, `pdf`, `mcp` unter `RootCommand`; `TimesheetQueryMixin`
  teilt die Optionen `--start/--end/--user/--all`; `TextRenderer`/`JsonRenderer` erzeugen die
  Ausgabe. Exit-Codes: 0 ok, 1 Laufzeitfehler, 2 Bedienfehler.
- `mcp` – Spring-AI-MCP-Server über stdio. Tools sind `@McpTool`-Methoden in
  `TimesheetTools` und `PdfTools`; `McpToolSupport.execute(...)` wandelt Ergebnisse/Exceptions
  in `CallToolResult` (Fehler als `isError`, Meldung ohne Framework-Präfix).
- `pdf` – `TimesheetPdfWriter` rendert je Projekt ein PDF über Thymeleaf
  (`templates/zeitnachweis.html`, per `--template` ersetzbar) und OpenHTMLtoPDF.

Wichtige Mechanik in `Application.main`: Ist das erste Argument `mcp`, wird **vor** picocli der
MCP-Server gestartet (Properties `spring.ai.mcp.server.enabled/stdio=true`, `keep-alive`
überschreiben das `enabled: false` aus `application.yaml`); `McpCommand` ist nur ein Stub für
die Hilfe. Sonst läuft picocli innerhalb des Spring-Kontexts.

**stdout-Disziplin:** stdout gehört der CLI-Ausgabe bzw. im MCP-Modus dem JSON-RPC-Protokoll.
Logs gehen über `logback-spring.xml` ausschließlich nach stderr (Level WARN). Nichts auf
stdout schreiben, was nicht Nutzdaten ist.

Versionsangabe: `application.yaml` wird beim Build gefiltert (`@project.version@`) und speist
sowohl `--version` (`VersionProvider`) als auch den MCP-Server-Namen/-Version.

## Tests

JUnit 5 mit Spring Boot Test. `KimaiClientTest` nutzt `MockRestServiceServer` (kein echter
Kimai-Zugriff); `ToolRegistrationTest` prüft, dass die `@McpTool`-Beans registriert werden;
`ApplicationTests` lädt den Kontext. Neue MCP-Tools also dort mit aufnehmen.
