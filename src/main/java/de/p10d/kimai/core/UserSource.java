package de.p10d.kimai.core;

import java.util.List;

/**
 * Vom Kern definierte Schnittstelle zur Userliste (Spec 002 FA-8, Plan E-3);
 * implementiert vom Kimai-Adapter.
 */
public interface UserSource {

    List<UserInfo> listUsers();
}
