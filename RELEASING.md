# Release

## Regeln

- [SemVer](https://semver.org/lang/de/): `MAJOR.MINOR.PATCH`.
- `main` trägt immer eine `-SNAPSHOT`-Version (die nächste geplante Version).
- Ein Release ist ein Git-Tag `vX.Y.Z` auf `main`. Die Pipeline (`release.yml`) setzt die
  Version beim Build aus dem Tag, deployt nach GitHub Packages und erzeugt das GitHub-Release.
  Es wird nichts auf `main` committet.

## Ablauf

1. `CHANGELOG.md`: Abschnitt „Unreleased“ in `## [X.Y.Z] – JJJJ-MM-TT` umbenennen, neuen
   leeren „Unreleased“-Abschnitt anlegen; in `pom.xml` `project.build.outputTimestamp` auf das
   Release-Datum setzen. Per PR auf `main` mergen.
2. Tag setzen und pushen:
   ```bash
   git checkout main && git pull
   git tag -a v0.1.0 -m "v0.1.0"
   git push origin v0.1.0
   ```
3. Pipeline unter *Actions → Release* beobachten. Ergebnis:
   - Maven-Artefakte `de.p10d:kimai-connect:X.Y.Z` (Jar, Sources, Javadoc, POM) in
     [GitHub Packages](https://github.com/pinguinsquad/kimai-connect/packages)
   - GitHub-Release `vX.Y.Z` mit generierten Release-Notes und `kimai-connect-X.Y.Z-exec.jar`
4. Falls die nächste Version eine andere Minor/Major ist: `pom.xml` per PR auf
   `X.Y+1.0-SNAPSHOT` heben.

Die Pipeline bricht ab, wenn der Tag nicht zur `-SNAPSHOT`-Version in `pom.xml` passt
(Tag `v0.2.0` erfordert `0.2.0-SNAPSHOT`), damit keine Version versehentlich übersprungen
oder doppelt vergeben wird.

## Korrektur eines fehlerhaften Releases

Veröffentlichte Versionen werden nicht überschrieben. Fehler werden mit einem neuen
Patch-Release behoben.
