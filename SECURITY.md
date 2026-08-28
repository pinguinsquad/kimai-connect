# Sicherheitshinweise

## Unterstützte Versionen

Sicherheitskorrekturen erhält nur die jeweils aktuelle Minor-Version.

## Meldung

Bitte melde Sicherheitsprobleme **nicht** als öffentliches Issue, sondern über
[GitHub Security Advisories](https://github.com/pinguinsquad/kimai-connect/security/advisories/new)
oder per E-Mail an kontakt@p10d.de. Du bekommst innerhalb von 7 Tagen eine Rückmeldung.

## Hinweise zum Betrieb

- `KIMAI_API_TOKEN` möglichst nicht in Dateien oder Shell-Historie ablegen; ein Secret-Manager (z. B. 1Password CLI mit `op run`) ist eine Option, die Bereitstellung bleibt aber dem Anwender überlassen.
- Der MCP-Server liest ausschließlich aus Kimai und schreibt nur PDFs ins lokale Dateisystem.
