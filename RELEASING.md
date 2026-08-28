# Release

## Branches

- `develop`: Integrationsbranch und Default-Branch. Trägt immer eine `-SNAPSHOT`-Version
  (die nächste geplante Version). Feature-Branches zweigen hier ab und werden per PR
  hierher gemergt.
- `main`: enthält ausschließlich Release-Stände. Jeder Commit auf `main` ist ein Release mit
  fester Version in `pom.xml` und Tag `vX.Y.Z`. Es wird nie direkt auf `main` gearbeitet.

## Regeln

- [SemVer](https://semver.org/lang/de/): `MAJOR.MINOR.PATCH`.
- Ein Release ist ein Git-Tag `vX.Y.Z` auf `main`. Die Pipeline (`release.yml`) prüft, dass der
  Tag auf `main` liegt und zur Version in `pom.xml` passt, deployt nach GitHub Packages und
  erzeugt das GitHub-Release. Die Pipeline committet nichts.

## Ablauf

1. Release-Branch von `develop` abzweigen, z. B. `release/0.1.0`:
   - `pom.xml`: Version von `0.1.0-SNAPSHOT` auf `0.1.0`, `project.build.outputTimestamp`
     auf das Release-Datum.
   - `CHANGELOG.md`: Abschnitt „Unreleased“ in `## [0.1.0] – JJJJ-MM-TT` umbenennen, neuen
     leeren „Unreleased“-Abschnitt anlegen, Vergleichslinks anpassen.
2. PR `release/0.1.0` → `main` öffnen; Review und grüne CI, dann mergen (Merge-Commit, kein
   Squash, damit die Historie von `develop` auf `main` erhalten bleibt).
3. Tag auf `main` setzen und pushen:
   ```bash
   git checkout main && git pull
   git tag -a v0.1.0 -m "v0.1.0"
   git push origin v0.1.0
   ```
4. Pipeline unter *Actions → Release* beobachten. Ergebnis:
   - Maven-Artefakte `de.p10d:kimai-connect:X.Y.Z` (Jar, Sources, Javadoc, POM) in
     [GitHub Packages](https://github.com/pinguinsquad/kimai-connect/packages)
   - GitHub-Release `vX.Y.Z` mit generierten Release-Notes und `kimai-connect-X.Y.Z-exec.jar`
5. `main` zurück nach `develop` mergen (PR `main` → `develop`) und dort `pom.xml` auf die
   nächste Version heben, z. B. `0.2.0-SNAPSHOT`.

Die Pipeline bricht ab, wenn der Tag nicht exakt der Version in `pom.xml` entspricht
(Tag `v0.2.0` erfordert `0.2.0`) oder der getaggte Commit nicht auf `main` liegt.

## Korrektur eines fehlerhaften Releases

Veröffentlichte Versionen werden nicht überschrieben. Fehler werden mit einem neuen
Patch-Release behoben: Branch `hotfix/X.Y.Z+1` von `main`, Fix, PR → `main`, Tag, danach
`main` → `develop` mergen.
