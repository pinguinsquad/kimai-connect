# kimai-connect

[![CI](https://github.com/pinguinsquad/kimai-connect/actions/workflows/ci.yml/badge.svg)](https://github.com/pinguinsquad/kimai-connect/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)

Anbindung an die Zeiterfassung [Kimai](https://www.kimai.org/) als Spring-Boot-Anwendung
(Java 25): Timesheets abrufen und erfassen, PDF-Zeitnachweise erzeugen und dieselben Funktionen
als [MCP](https://modelcontextprotocol.io/)-Server für KI-Clients bereitstellen. Das Jar ist
zugleich als Bibliothek nutzbar, etwa um aus Kimai-Zeiten Rechnungsentwürfe zu erzeugen.

## Funktionen

| Oberfläche | Funktion |
|---|---|
| CLI `list` | Abrechenbare Timesheet-Einträge eines Zeitraums als Tabelle oder JSON |
| CLI `add`  | Einen Timesheet-Eintrag erfassen (Projekt und Tätigkeit per Name oder ID) |
| CLI `projects`, `activities` | Projekte und Tätigkeiten nachschlagen |
| CLI `pdf`  | Pro Projekt ein PDF-Tätigkeitsnachweis (Kopfdaten, Summen je Tätigkeit, alle Einträge) |
| CLI `mcp`  | MCP-Server über stdio |
| MCP-Tool `kimai_list_timesheets` | Einträge eines Zeitraums, optional je User, optional inkl. nicht abrechenbarer |
| MCP-Tool `kimai_list_users` | Alle Kimai-User mit ID und Name |
| MCP-Tool `kimai_list_projects`, `kimai_list_activities` | Projekte und Tätigkeiten nachschlagen |
| MCP-Tool `kimai_create_timesheet` | Einen Timesheet-Eintrag erfassen wie `add` |
| MCP-Tool `kimai_generate_timesheet_pdfs` | PDF-Zeitnachweise wie `pdf` |

Gegenüber Kimai wird **nur beim Erfassen** (`add`, `kimai_create_timesheet`) geschrieben, und
zwar ausschließlich neue Einträge – Bearbeiten und Löschen gibt es nicht. Alles andere ist
lesend; lokal entstehen nur PDFs.

## Voraussetzungen

- Java 25 zur Laufzeit; zum Bauen zusätzlich Maven 3.9 (im Repo per [asdf](https://asdf-vm.com/)
  über `.tool-versions`)
- Ein Kimai-API-Token (Kimai → Benutzer → API-Zugriff)

## Konfiguration

Die Anwendung wird ausschließlich über zwei Umgebungsvariablen konfiguriert:

| Variable | Bedeutung |
|---|---|
| `KIMAI_BASE_URL` | Pflicht. Basis-URL der Kimai-API, z. B. `https://kimai.example.org/api` |
| `KIMAI_API_TOKEN` | Pflicht. API-Token des Kimai-Users |

Wie das Token in die Umgebung gelangt, bleibt dem Anwender überlassen – direkt beim Aufruf,
per `export`, aus einem Secret-Manager. Mit der
[1Password CLI](https://developer.1password.com/docs/cli/) etwa als `op://`-Referenz, die
`op run` beim Start auflöst:

```bash
KIMAI_API_TOKEN="op://<Tresor>/<Eintrag>/<Feld>" op run --no-masking -- java -jar kimai-connect-<version>-exec.jar …
```

## Build

```bash
mvn verify
```

Erzeugt `target/kimai-connect-<version>.jar` (Bibliothek) und
`target/kimai-connect-<version>-exec.jar` (eigenständig lauffähig).

## Verwendung

Die Beispiele setzen die Variablen einmal und kürzen den Aufruf mit einem Alias ab:

```bash
export KIMAI_BASE_URL=https://kimai.example.org/api
export KIMAI_API_TOKEN=<token>
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

### Zeiten erfassen

```bash
kimai add --date 2026-08-28 --start 09:00 --end 12:30 --project "Projekt X" --activity Entwicklung \
  --description "Beratung vor Ort" [--tag a --tag b] [--user 3] [--not-billable] [--json]
kimai add --start 14:00 --duration 1h30m --project 12 --activity 5      # heute, Dauer statt Ende
```

Projekt und Tätigkeit werden per Name oder Kimai-ID angegeben. Ein Name muss eindeutig sein –
erst zählt der exakte Name, sonst ein Namensbestandteil; bei mehreren Treffern bricht der Aufruf
mit den Kandidaten ab, bei keinem mit den verfügbaren Einträgen. Tätigkeiten werden im Projekt
gesucht, globale eingeschlossen. Die Dauer versteht `3h30m`, `2h`, `45m` und `1:30`. `--user`
setzt einen anderen Kimai-User voraus, für den der API-User Einträge anlegen darf.

Die Ausgabe zeigt den Eintrag so, wie Kimai ihn gespeichert hat – Rundungsregeln der
Kimai-Konfiguration können Beginn, Ende und Dauer verändern.

Zum Nachschlagen:

```bash
kimai projects [--customer "ACME"] [--json]        # ID, Kunde, Projekt
kimai activities [--project "Projekt X"] [--json]  # ID, Tätigkeit, Projekt (oder global)
```

### PDF-Zeitnachweise

```bash
kimai pdf --start 2026-07-01 --end 2026-07-31 [--user 3] [--all] [--out pdf/] [--template eigenes.html]
```

Das Layout stammt aus `src/main/resources/templates/zeitnachweis.html` (Thymeleaf) und kann
per `--template` durch eine eigene Datei ersetzt werden.

### MCP-Server

`kimai mcp` startet den Server über stdio; stdout gehört dem Protokoll, Logs gehen auf stderr.
Der Server braucht nur die beiden Umgebungsvariablen, kein weiteres Tooling.

Claude Code:

```bash
claude mcp add kimai \
  --env KIMAI_BASE_URL=https://kimai.example.org/api \
  --env KIMAI_API_TOKEN=<token> -- \
  java -jar /pfad/zu/kimai-connect-<version>-exec.jar mcp
```

Clients mit `mcp.json` (Claude Desktop, LM Studio, Cursor, …):

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

Soll das Token nicht in der Client-Konfiguration stehen, lässt sich ein Secret-Manager
vorschalten – mit 1Password: `"command": "op"`, `"args": ["run", "--no-masking", "--", "java",
"-jar", "…", "mcp"]` und `"KIMAI_API_TOKEN": "op://<Tresor>/<Eintrag>/<Feld>"`. Viele Clients
starten ohne Shell-`PATH`; `java` und `op` dann mit absolutem Pfad angeben.

## Als Bibliothek einbinden

Releases liegen in GitHub Packages. In `~/.m2/settings.xml` ist dafür ein GitHub-Token mit
`read:packages` nötig:

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
  <version>0.2.0</version>
</dependency>
```

Das Kern-Datenmodell liegt in `de.p10d.kimai.core`: `TimesheetService` mit `TimesheetQuery`,
`TimesheetReport`, `TimesheetEntry` zum Lesen; `TimeTrackingService` mit `TimesheetDraft`,
`NewTimesheet`, `CreatedTimesheet`, `ProjectInfo`, `ActivityInfo` zum Erfassen. Der Kimai-Client
implementiert die dort definierten Schnittstellen `TimesheetSource`, `UserSource`,
`ProjectSource`, `ActivitySource` und `TimesheetWriter`.

## Mitmachen

Siehe [CONTRIBUTING.md](CONTRIBUTING.md). Sicherheitsprobleme bitte gemäß
[SECURITY.md](SECURITY.md) melden.

## Lizenz

[Apache License 2.0](LICENSE). Drittanbieter-Komponenten und deren Lizenzen: [NOTICE](NOTICE)
sowie `META-INF/kimai-connect-THIRD-PARTY.txt` in jedem gebauten Jar (der Build bricht bei
GPL-/AGPL-lizenzierten Abhängigkeiten ab).
