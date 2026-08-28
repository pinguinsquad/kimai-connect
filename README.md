# kimai-connect

[![CI](https://github.com/pinguinsquad/kimai-connect/actions/workflows/ci.yml/badge.svg)](https://github.com/pinguinsquad/kimai-connect/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)

Anbindung an die Zeiterfassung [Kimai](https://www.kimai.org/) als Spring-Boot-Anwendung
(Java 25): Timesheets abrufen, PDF-Zeitnachweise erzeugen und dieselben Funktionen als
[MCP](https://modelcontextprotocol.io/)-Server für KI-Clients (z. B. Claude Code) bereitstellen.
Das Jar ist zugleich als Bibliothek nutzbar – so baut
[kimai2lexware-v2](https://github.com/pinguinsquad/dies-und-das/tree/main/kimai2lexware-v2)
darauf Rechnungsentwürfe für Lexware.

## Funktionen

| Oberfläche | Funktion |
|---|---|
| CLI `list` | Abrechenbare Timesheet-Einträge eines Zeitraums als Tabelle oder JSON |
| CLI `pdf`  | Pro Projekt ein PDF-Tätigkeitsnachweis (Kopfdaten, Summen je Tätigkeit, alle Einträge) |
| CLI `mcp`  | MCP-Server über stdio |
| MCP-Tool `kimai_list_timesheets` | Einträge eines Zeitraums, optional je User, optional inkl. nicht abrechenbarer |
| MCP-Tool `kimai_list_users` | Alle Kimai-User mit ID und Name |
| MCP-Tool `kimai_generate_timesheet_pdfs` | PDF-Zeitnachweise wie `pdf` |

Alle Funktionen sind **lesend** gegenüber Kimai; geschrieben wird nur lokal (PDFs).

## Voraussetzungen

- Java 25 und Maven 3.9 (im Repo per [asdf](https://asdf-vm.com/) über `.tool-versions`)
- Ein Kimai-API-Token (Kimai → Benutzer → API-Zugriff)

## Konfiguration

| Variable | Bedeutung |
|---|---|
| `KIMAI_BASE_URL` | Pflicht. Basis-URL der Kimai-API, z. B. `https://kimai.example.org/api` |
| `KIMAI_API_TOKEN` | Pflicht. API-Token des Kimai-Users |

Beide Werte werden als Umgebungsvariablen gelesen – wie sie dorthin kommen, entscheidet der
Anwender. Direkt beim Aufruf:

```bash
KIMAI_BASE_URL=https://kimai.example.org/api KIMAI_API_TOKEN=<token> java -jar kimai-connect-<version>-exec.jar list ...
```

Damit das Token nicht in Shell-Historie oder Dateien landet, bietet sich ein Secret-Manager
an. Mit der [1Password CLI](https://developer.1password.com/docs/cli/) etwa als
`op://`-Referenz plus `op run`:

```bash
export KIMAI_API_TOKEN="op://<Tresor>/<Eintrag>/<Feld>"
op run --no-masking -- java -jar kimai-connect-<version>-exec.jar list ...
```

Die Beispiele unten verwenden ein `kimai`-Alias, damit sie unabhängig von der gewählten
Variante lesbar bleiben.

## Build

```bash
mvn verify
```

Erzeugt `target/kimai-connect-<version>.jar` (Bibliothek) und
`target/kimai-connect-<version>-exec.jar` (eigenständig lauffähig).

## Verwendung

```bash
export KIMAI_BASE_URL=https://kimai.example.org/api
export KIMAI_API_TOKEN=<token>            # oder eine op://-Referenz, dann mit „op run --no-masking --“ davor
alias kimai='java -jar target/kimai-connect-<version>-exec.jar'
```

### Einträge auflisten

```bash
kimai list --start 2026-07-01 --end 2026-07-31            # Tabelle, nur abrechenbar, alle User
kimai list --start 2026-07-01 --end 2026-07-31 --user 3 --all --json
```

Optionen: `--start`/`--end` (Pflicht, `yyyy-MM-dd`, beide Tage einschließlich),
`--user <Kimai-User-ID>`, `--all` (auch nicht Abrechenbares), `--json`.
Exit-Codes: 0 Erfolg, 1 Laufzeitfehler, 2 Bedienfehler.

### PDF-Zeitnachweise

```bash
kimai pdf --start 2026-07-01 --end 2026-07-31 [--user 3] [--all] [--out pdf/] [--template eigenes.html]
```

Das Layout stammt aus `src/main/resources/templates/zeitnachweis.html` (Thymeleaf) und kann
per `--template` durch eine eigene Datei ersetzt werden.

### MCP-Server

`kimai mcp` startet den Server über stdio; stdout gehört dem Protokoll, Logs gehen auf stderr.
Einbindung in Claude Code:

```bash
claude mcp add kimai \
  --env KIMAI_BASE_URL=https://kimai.example.org/api \
  --env KIMAI_API_TOKEN=<token> -- \
  java -jar /pfad/zu/kimai-connect-<version>-exec.jar mcp
```

Andere MCP-Clients (Claude Desktop, LM Studio, Cursor, …) lesen eine `mcp.json`; der Server
braucht nur die beiden Umgebungsvariablen, kein weiteres Tooling:

```json
{
  "mcpServers": {
    "kimai": {
      "command": "java",
      "args": ["-jar", "/pfad/zu/kimai-connect-<version>-exec.jar", "mcp"],
      "env": {
        "KIMAI_BASE_URL": "https://kimai.example.org/api",
        "KIMAI_API_TOKEN": "<token>"
      }
    }
  }
}
```

Wer das Token nicht in die Client-Konfiguration schreiben will, kann einen Secret-Manager
vorschalten, z. B. 1Password: `"command": "op"`, `"args": ["run", "--no-masking", "--",
"java", "-jar", "…", "mcp"]` und `"KIMAI_API_TOKEN": "op://<Tresor>/<Eintrag>/<Feld>"`.
Manche Clients haben keinen Shell-`PATH` – dann `java` und `op` mit absolutem Pfad angeben.

## Als Bibliothek einbinden

Releases werden nach GitHub Packages veröffentlicht. In `~/.m2/settings.xml` ist dafür ein
GitHub-Token mit `read:packages` nötig:

```xml
<servers>
  <server>
    <id>github-kimai-connect</id>
    <username>GITHUB-BENUTZERNAME</username>
    <password>TOKEN</password>
  </server>
</servers>
```

Im Projekt:

```xml
<repositories>
  <repository>
    <id>github-kimai-connect</id>
    <url>https://maven.pkg.github.com/pinguinsquad/kimai-connect</url>
  </repository>
</repositories>

<dependency>
  <groupId>de.p10d</groupId>
  <artifactId>kimai-connect</artifactId>
  <version>0.1.0</version>
</dependency>
```

Das Kern-Datenmodell liegt in `de.p10d.kimai.core` (`TimesheetService`, `TimesheetQuery`,
`TimesheetReport`, `TimesheetEntry`); der Kimai-Client implementiert die dort definierten
Schnittstellen `TimesheetSource` und `UserSource`.

## Mitmachen

Siehe [CONTRIBUTING.md](CONTRIBUTING.md). Sicherheitsprobleme bitte gemäß
[SECURITY.md](SECURITY.md) melden.

## Lizenz

[Apache License 2.0](LICENSE)
