# Release

## Modell

- Es gibt nur den Branch `main`. Jeder Commit auf `main` ist ein Release.
- Arbeit passiert auf Feature-Branches; ein PR gegen `main` hebt die Version in `pom.xml`
  selbst und pflegt den CHANGELOG-Abschnitt. Es gibt keine `-SNAPSHOT`-Versionen.
- Die Pipeline (`release.yml`) läuft bei jedem Push auf `main`: sie liest die Version aus
  `pom.xml`, legt den Tag `vX.Y.Z` an, deployt nach GitHub Packages und erzeugt das
  GitHub-Release. Existiert der Tag schon, bricht sie ab – die Version muss also in jedem
  PR erhöht werden.

## Regeln

- [SemVer](https://semver.org/lang/de/): `MAJOR.MINOR.PATCH`. Neue Funktion → Minor,
  Fehlerbehebung oder Dependency-Update → Patch, inkompatible Änderung → Major.
- Zwei offene PRs, die dieselbe Version beanspruchen: wer zweiter mergt, rebased und hebt
  auf die nächste Version.

## Ablauf pro PR

1. Feature-Branch von `main` abzweigen, Änderung umsetzen, Tests ergänzen.
2. `pom.xml`: `<version>` auf die neue Version, `project.build.outputTimestamp` auf das
   aktuelle Datum.
3. `CHANGELOG.md`: neuen Abschnitt `## [X.Y.Z] – JJJJ-MM-TT` mit den Änderungen anlegen und
   den Vergleichslink am Dateiende ergänzen.
4. PR gegen `main`; Review und grüne CI, dann mergen.
5. Pipeline unter *Actions → Release* beobachten. Ergebnis:
   - Tag `vX.Y.Z` auf dem Merge-Commit
   - Maven-Artefakte `de.p10d:kimai-connect:X.Y.Z` (Jar, Sources, Javadoc, POM) in
     [GitHub Packages](https://github.com/pinguinsquad/kimai-connect/packages)
   - GitHub-Release `vX.Y.Z` mit generierten Release-Notes und `kimai-connect-X.Y.Z-exec.jar`

## Dependabot

Dependabot läuft monatlich und bündelt Updates in Gruppen. Dependabot erhöht die Version in
`pom.xml` nicht selbst – vor dem Merge eines Dependabot-PRs die Patch-Version und den CHANGELOG
im PR nachziehen (Branch auschecken, Commit dazu, pushen).

## Korrektur eines fehlerhaften Releases

Veröffentlichte Versionen werden nicht überschrieben. Fehler werden mit einem neuen
Patch-Release behoben – also ganz normal per PR gegen `main`.
