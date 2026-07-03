package ch.admin.bit.jme.jwe.person;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory store — this example is about the encrypted transport, not about persistence.
 */
@Service
public class PersonService {

    private final Map<UUID, Person> persons = new ConcurrentHashMap<>();

    public PersonService() {
        create(new Person(null, "Henriette", "Muster", "756.1234.5678.97"));
        create(new Person(null, "Hans", "Beispiel", "756.9876.5432.10"));
    }

    public List<Person> findAll() {
        return persons.values().stream()
                .sorted(Comparator.comparing(Person::lastName).thenComparing(Person::firstName))
                .toList();
    }

    public Person create(Person person) {
        Person created = person.withId(UUID.randomUUID());
        persons.put(created.id(), created);
        return created;
    }
}
