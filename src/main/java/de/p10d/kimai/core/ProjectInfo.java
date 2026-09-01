package de.p10d.kimai.core;

/**
 * Ein Kimai-Projekt für die Auswahl beim Erfassen von Zeiten.
 *
 * @param id           Kimai-Projekt-ID
 * @param name         Projektname
 * @param customerId   ID des Kunden, null wenn unbekannt
 * @param customerName Name des Kunden, null wenn unbekannt
 */
public record ProjectInfo(long id, String name, Long customerId, String customerName) {
}
