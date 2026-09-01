# Changelog

Format nach [Keep a Changelog](https://keepachangelog.com/de/1.1.0/), Versionierung nach [SemVer](https://semver.org/lang/de/).

## [Unreleased]

## [0.2.0] – 2026-09-01

Zeiten erfassen (#20).

### Hinzugefügt
- CLI `add`: Timesheet-Eintrag anlegen mit Datum, Beginn, Ende oder Dauer (`3h30m`, `1:30`),
  Projekt und Tätigkeit per Name oder ID, Beschreibung, Tags, User, `--not-billable`, `--json`
- CLI `projects` und `activities` zum Nachschlagen von Namen und IDs
- MCP-Tools `kimai_list_projects`, `kimai_list_activities`, `kimai_create_timesheet`
- Kern: `TimeTrackingService` mit Namensauflösung (eindeutiger Treffer, sonst Fehler mit
  Kandidaten), Ports `ProjectSource`, `ActivitySource`, `TimesheetWriter`, Records
  `TimesheetDraft`, `NewTimesheet`, `CreatedTimesheet`, `ProjectInfo`, `ActivityInfo`,
  `DurationParser`
- `KimaiClient` hängt Validierungsfehler aus Kimai-Fehlerantworten an die Meldung an

### Geändert
- kimai-connect ist nicht mehr rein lesend: `POST /timesheets` beim Erfassen; Bearbeiten und
  Löschen bleiben außen vor. Doku (README, SECURITY) entsprechend angepasst

## [0.1.3] – 2026-09-01

### Geändert
- `openhtmltopdf-pdfbox` 1.1.79 → 1.1.83 (Seitenumbruch-Korrekturen bei Tabellen und Blöcken)
- `license-maven-plugin` 2.5.0 → 2.7.1

## [0.1.2] – 2026-09-01

### Geändert
- CI: `dependabot/fetch-metadata` 2 → 3

## [0.1.1] – 2026-09-01

Repository öffentlich.

### Geändert
- Doku ohne Bezüge auf interne Instanzen und private Repositories; lokale Hinweise für
  Claude Code in `CLAUDE.local.md` (gitignored)

## [0.1.0] – 2026-09-01

Erstes Release.

### Hinzugefügt
- CLI `list`, `pdf`, `mcp`; MCP-Tools `kimai_list_timesheets`, `kimai_list_users`, `kimai_generate_timesheet_pdfs`
- Bibliotheks-Jar mit Kern-Datenmodell `de.p10d.kimai.core`
- CI (`mvn verify`), Release-Pipeline nach GitHub Packages (Jar, Sources, Javadoc, POM)
- Drittlizenz-Report `META-INF/kimai-connect-THIRD-PARTY.txt` im Jar (`license-maven-plugin`);
  Build bricht bei GPL/AGPL-Abhängigkeiten ab; NOTICE nennt LGPL-/EPL-Komponenten

### Geändert
- `KIMAI_BASE_URL` ist Pflicht, kein eingebauter Default

[Unreleased]: https://github.com/pinguinsquad/kimai-connect/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/pinguinsquad/kimai-connect/compare/v0.1.3...v0.2.0
[0.1.3]: https://github.com/pinguinsquad/kimai-connect/compare/v0.1.2...v0.1.3
[0.1.2]: https://github.com/pinguinsquad/kimai-connect/compare/v0.1.1...v0.1.2
[0.1.1]: https://github.com/pinguinsquad/kimai-connect/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/pinguinsquad/kimai-connect/releases/tag/v0.1.0
