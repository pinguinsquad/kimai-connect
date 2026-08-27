# Mitmachen

Danke für dein Interesse! Beiträge laufen über Issues und Pull Requests.

## Ablauf

1. Issue anlegen oder ein bestehendes kommentieren, bevor größere Änderungen entstehen.
2. Branch von `main` abzweigen, Änderung umsetzen, Tests ergänzen.
3. `mvn verify` muss lokal grün sein – die CI prüft dasselbe.
4. Pull Request gegen `main`; ein Review und grüne CI sind Voraussetzung fürs Mergen.

## Konventionen

- **Sprache:** Dokumentation, Commit-Messages und Kommentare auf Deutsch; Code-Bezeichner auf Englisch.
- **Commits:** kurze, sachliche Betreffzeile im Präsens („README ergänzt“ statt „adds README“);
  Bezug auf Issues mit `Closes #n`.
- **Tests:** Fachlogik in `core` wird ohne Netzzugang getestet; der Kimai-Client wird gemockt.
- **Lizenz-Header:** Es werden keine Lizenz-Header in Quelldateien verwendet. Mit einem
  Beitrag erklärst du dich einverstanden, dass er unter der [Apache License 2.0](LICENSE)
  veröffentlicht wird.
- **Werkzeuge:** Java 25 und Maven per `.tool-versions` (asdf).

## Release

Siehe [RELEASING.md](RELEASING.md).
