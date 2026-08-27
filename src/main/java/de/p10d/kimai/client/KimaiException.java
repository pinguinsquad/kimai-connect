package de.p10d.kimai.client;

/**
 * Fehler beim Zugriff auf die Kimai-API. Die Meldung ist für die Ausgabe an
 * den Nutzer gedacht und enthält nie das Token (Spec 001 FA-8).
 */
public class KimaiException extends RuntimeException {

    public KimaiException(String message) {
        super(message);
    }

    public KimaiException(String message, Throwable cause) {
        super(message, cause);
    }
}
