package ch.admin.bit.jme.jwe.web;

import ch.admin.bit.jme.jwe.person.Person;
import ch.admin.bit.jme.jwe.person.PersonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints under {@code /api/persons} match the JWE filter's included paths: the browser sends
 * the POST body as a compact JWE and receives all responses encrypted with the request-local
 * response key ({@code JWE-Response-Key} header). Controllers see plain JSON — encryption and
 * decryption happen transparently in the JWE servlet filter, after OAuth2 authentication.
 */
@RestController
@RequestMapping("/api/persons")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;

    /**
     * Encrypted GET: no request body, but the response is returned as {@code application/jose},
     * encrypted with the key the client supplied in the {@code JWE-Response-Key} header.
     */
    @GetMapping
    @PreAuthorize("hasRole('person','read')")
    public List<Person> getPersons() {
        return personService.findAll();
    }

    /**
     * Encrypted POST: the request body arrives as {@code application/jose} and is decrypted by
     * the JWE servlet filter before it reaches this method; the response is encrypted again.
     */
    @PostMapping
    @PreAuthorize("hasRole('person','write')")
    @ResponseStatus(HttpStatus.CREATED)
    public Person createPerson(@RequestBody Person person) {
        return personService.create(person);
    }
}
