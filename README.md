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

Tokens gehören nicht in Shell-Historie oder Dateien. Bewährt hat sich die
[1Password CLI](https://developer.1password.com/docs/cli/): Variablen als `op://`-Referenz
setzen und das Programm mit `op run` starten – die Beispiele unten nutzen dieses Muster.

## Build

```bash
mvn verify
```

Erzeugt `target/kimai-connect-<version>.jar` (Bibliothek) und
`target/kimai-connect-<version>-exec.jar` (eigenständig lauffähig).

## Verwendung

```bash
export KIMAI_BASE_URL=https://kimai.example.org/api
export KIMAI_API_TOKEN="op://<Tresor>/<Eintrag>/<Feld>"
alias kimai='op run --no-masking -- java -jar target/kimai-connect-0.1.0-SNAPSHOT-exec.jar'
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
  --env KIMAI_API_TOKEN="op://<Tresor>/<Eintrag>/<Feld>" -- \
  op run --no-masking -- java -jar /pfad/zu/kimai-connect-0.1.0-exec.jar mcp
```

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

[Apache License 2.0](LICENSE). Drittanbieter-Komponenten und deren Lizenzen: [NOTICE](NOTICE)
sowie `META-INF/kimai-connect-THIRD-PARTY.txt` in jedem gebauten Jar (der Build bricht bei
GPL-/AGPL-lizenzierten Abhängigkeiten ab).
