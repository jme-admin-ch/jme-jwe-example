package ch.admin.bit.jme.jwe.person;

import java.util.UUID;

/**
 * Example payload with personal data worth protecting in transit — the AHV number never crosses
 * the wire in plaintext because {@code /api/persons} is within the JWE filter's included paths.
 */
public record Person(UUID id, String firstName, String lastName, String ahvNumber) {

    public Person withId(UUID newId) {
        return new Person(newId, firstName, lastName, ahvNumber);
    }
}
