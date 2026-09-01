# Changelog

Format nach [Keep a Changelog](https://keepachangelog.com/de/1.1.0/), Versionierung nach [SemVer](https://semver.org/lang/de/).

## [Unreleased]

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

[Unreleased]: https://github.com/pinguinsquad/kimai-connect/compare/v0.1.2...HEAD
[0.1.2]: https://github.com/pinguinsquad/kimai-connect/compare/v0.1.1...v0.1.2
[0.1.1]: https://github.com/pinguinsquad/kimai-connect/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/pinguinsquad/kimai-connect/releases/tag/v0.1.0
