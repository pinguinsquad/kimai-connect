# Sicherheitshinweise

## Unterstützte Versionen

Sicherheitskorrekturen erhält nur die jeweils aktuelle Minor-Version.

## Meldung

Bitte melde Sicherheitsprobleme **nicht** als öffentliches Issue, sondern über
[GitHub Security Advisories](https://github.com/pinguinsquad/kimai-connect/security/advisories/new)
oder per E-Mail an kontakt@p10d.de. Du bekommst innerhalb von 7 Tagen eine Rückmeldung.

## Hinweise zum Betrieb

- Das API-Token wird nur aus der Umgebungsvariable `KIMAI_API_TOKEN` gelesen. Es sollte nicht in
  Dateien oder der Shell-Historie landen; ein Secret-Manager wie die 1Password CLI ist dafür eine
  Möglichkeit.
- kimai-connect liest aus Kimai und schreibt dort nur beim Erfassen neuer Timesheet-Einträge
  (`add`, `kimai_create_timesheet`); bestehende Einträge werden nie verändert oder gelöscht.
  Lokal entstehen nur PDFs.
- Was der API-User erfassen darf (etwa Einträge für andere User), regeln die Berechtigungen
  in Kimai; ein Token mit möglichst wenigen Rechten ist empfehlenswert.
