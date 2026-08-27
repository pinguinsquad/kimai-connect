# Sicherheitshinweise

## Unterstützte Versionen

Sicherheitskorrekturen erhält nur die jeweils aktuelle Minor-Version.

## Meldung

Bitte melde Sicherheitsprobleme **nicht** als öffentliches Issue, sondern über
[GitHub Security Advisories](https://github.com/pinguinsquad/kimai-connect/security/advisories/new)
oder per E-Mail an kontakt@p10d.de. Du bekommst innerhalb von 7 Tagen eine Rückmeldung.

## Hinweise zum Betrieb

- `KIMAI_API_TOKEN` nie in Dateien oder Shell-Historie ablegen; Secret-Manager wie die 1Password CLI verwenden.
- Der MCP-Server liest ausschließlich aus Kimai und schreibt nur PDFs ins lokale Dateisystem.
