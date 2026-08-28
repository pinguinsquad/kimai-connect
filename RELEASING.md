# Release

Es gibt nur den Branch `main`, und jeder Commit darauf ist ein Release. Entwickelt wird auf
Feature-Branches; der Pull Request bringt die neue Version und den CHANGELOG-Eintrag mit.
`-SNAPSHOT`-Versionen gibt es nicht.

## Versionierung

[SemVer](https://semver.org/lang/de/) `MAJOR.MINOR.PATCH`: neue Funktion → Minor,
Fehlerbehebung oder Dependency-Update → Patch, inkompatible Änderung → Major.

Beanspruchen zwei offene Pull Requests dieselbe Version, rebased der zweite nach dem Merge des
ersten und hebt auf die nächste Version.

## Ablauf

1. Feature-Branch von `main` abzweigen, Änderung umsetzen, Tests ergänzen.
2. `pom.xml`: `<version>` auf die neue Version, `project.build.outputTimestamp` auf das
   aktuelle Datum.
3. `CHANGELOG.md`: Abschnitt `## [X.Y.Z] – JJJJ-MM-TT` mit den Änderungen anlegen und den
   Vergleichslink am Dateiende ergänzen.
4. Pull Request gegen `main`; Review und grüne CI, dann mergen.

Der Merge löst die Pipeline `release.yml` aus. Sie liest die Version aus `pom.xml`, deployt nach
GitHub Packages, setzt den Tag `vX.Y.Z` auf den Merge-Commit und legt das GitHub-Release an:

- Maven-Artefakte `de.p10d:kimai-connect:X.Y.Z` (Jar, Sources, Javadoc, POM) in
  [GitHub Packages](https://github.com/pinguinsquad/kimai-connect/packages)
- GitHub-Release `vX.Y.Z` mit generierten Release-Notes und `kimai-connect-X.Y.Z-exec.jar`

Die Pipeline bricht ab, wenn die Version auf `-SNAPSHOT` endet oder ihr Tag bereits existiert –
eine vergessene Versionserhöhung fällt also sofort auf.

## Dependabot

Dependabot läuft monatlich und bündelt Updates je Ökosystem in einem Pull Request. Es erhöht die
Version in `pom.xml` nicht selbst; vor dem Merge den Branch auschecken, Patch-Version und
CHANGELOG ergänzen und pushen.

## Fehlerhafte Releases

Veröffentlichte Versionen werden nicht überschrieben. Ein Fehler wird mit einem Patch-Release
behoben – wie jede andere Änderung per Pull Request gegen `main`.
