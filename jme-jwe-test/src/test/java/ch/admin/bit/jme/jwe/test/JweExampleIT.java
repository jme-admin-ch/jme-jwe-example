package ch.admin.bit.jme.jwe.test;

import ch.admin.bit.jeap.jme.test.BootServiceSpringIntegrationTestBase;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import static io.restassured.RestAssured.given;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration test covering the local Docker Compose setup: Spring Boot's Docker
 * Compose support starts Vault (see {@code src/test/resources/application.yml}), the OAuth mock
 * server and the JWE SCS are started as Maven subprocesses, and the test then exercises the
 * three showcased scenarios against the running system with real Vault-backed keys:
 * <ul>
 *     <li>Encrypted POST to {@code /api/persons}</li>
 *     <li>Encrypted GET of {@code /api/persons}</li>
 *     <li>Unencrypted (allowlisted) GET of {@code /api/public/info}</li>
 * </ul>
 */
@SuppressWarnings("java:S112") // test helpers chain Nimbus/JCA calls with distinct checked exceptions
class JweExampleIT extends BootServiceSpringIntegrationTestBase {

    private static final String AUTH_BASE_URL = "http://localhost:8081/jme-jwe-auth-scs";
    private static final String SCS_BASE_URL = "http://localhost:8080/jme-jwe-scs";
    private static final String AUTH_TOKEN_URL = AUTH_BASE_URL + "/oauth2/token";
    private static final String VAULT_TRANSIT_KEY_NAME = "jme-jwe-scs-key";

    private static final String APPLICATION_JOSE = "application/jose";
    private static final String HEADER_RESPONSE_KEY = "JWE-Response-Key";

    @BeforeAll
    static void startServices() throws Exception {
        startService("jme-jwe-auth-scs", AUTH_BASE_URL);
        startService("jme-jwe-scs/jme-jwe-scs-web", SCS_BASE_URL);
    }

    @Test
    void jwksIsServedFromVaultTransitKey() throws Exception {
        RSAKey publicKey = jwePublicKey();

        assertThat(publicKey.getKeyID()).startsWith(VAULT_TRANSIT_KEY_NAME + ":");
        assertThat(publicKey.getAlgorithm().getName()).isEqualTo("RSA-OAEP-256");
    }

    @Test
    void encryptedPostCreatesPersonAndReturnsEncryptedResponse() throws Exception {
        RSAKey publicKey = jwePublicKey();
        SecretKey responseCek = aes256();
        String personJson = "{\"firstName\":\"Vera\",\"lastName\":\"Vault\",\"ahvNumber\":\"756.1111.2222.33\"}";

        byte[] encryptedResponse = given()
                .header("Authorization", "Bearer " + accessToken())
                .header("Content-Type", APPLICATION_JOSE)
                .header("Accept", APPLICATION_JOSE)
                .header(HEADER_RESPONSE_KEY, responseKeyEnvelope(publicKey, responseCek))
                .body(encryptRequest(publicKey, personJson).getBytes(US_ASCII))
                .post(SCS_BASE_URL + "/api/persons")
                .then().statusCode(201)
                .contentType(APPLICATION_JOSE)
                .extract().asByteArray();

        assertThat(decrypt(encryptedResponse, responseCek))
                .contains("\"lastName\":\"Vault\"")
                .contains("\"id\":");
    }

    @Test
    void encryptedGetReturnsPersonsEncryptedWithResponseKey() throws Exception {
        RSAKey publicKey = jwePublicKey();
        SecretKey responseCek = aes256();

        byte[] encryptedResponse = given()
                .header("Authorization", "Bearer " + accessToken())
                .header("Accept", APPLICATION_JOSE)
                .header(HEADER_RESPONSE_KEY, responseKeyEnvelope(publicKey, responseCek))
                .get(SCS_BASE_URL + "/api/persons")
                .then().statusCode(200)
                .contentType(APPLICATION_JOSE)
                .extract().asByteArray();

        assertThat(decrypt(encryptedResponse, responseCek)).contains("Muster");
    }

    @Test
    void allowlistedPublicInfoIsServedAsPlainJson() {
        given()
                .header("Authorization", "Bearer " + accessToken())
                .get(SCS_BASE_URL + "/api/public/info")
                .then().statusCode(200)
                .contentType(ContentType.JSON)
                .body("application", org.hamcrest.Matchers.equalTo("jme-jwe-scs"));
    }

    @Test
    void plaintextRequestToEncryptedEndpointIsRejected() {
        given()
                .header("Authorization", "Bearer " + accessToken())
                .header("Accept", "application/json")
                .get(SCS_BASE_URL + "/api/persons")
                .then().statusCode(406);
    }

    // --- Helpers ----------------------------------------------------------------------------------

    private static String accessToken() {
        return given()
                .contentType(ContentType.URLENC)
                .formParam("grant_type", "client_credentials")
                .formParam("client_id", "jme-jwe-it-client")
                .formParam("client_secret", "secret")
                .post(AUTH_TOKEN_URL)
                .then().statusCode(200)
                .extract().jsonPath().getString("access_token");
    }

    private static RSAKey jwePublicKey() throws Exception {
        String jwks = given().get(SCS_BASE_URL + "/.well-known/jwks.json")
                .then().statusCode(200)
                .extract().asString();
        return JWKSet.parse(jwks).getKeys().get(0).toRSAKey();
    }

    private static SecretKey aes256() throws Exception {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(256);
        return generator.generateKey();
    }

    private static String encryptRequest(RSAKey publicKey, String json) throws Exception {
        JWEObject jwe = new JWEObject(
                new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A256GCM)
                        .keyID(publicKey.getKeyID()).contentType("application/json").build(),
                new Payload(json));
        jwe.encrypt(new RSAEncrypter(publicKey.toRSAPublicKey()));
        return jwe.serialize();
    }

    private static String responseKeyEnvelope(RSAKey publicKey, SecretKey cek) throws Exception {
        JWEObject envelope = new JWEObject(
                new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A256GCM)
                        .keyID(publicKey.getKeyID()).build(),
                new Payload(cek.getEncoded()));
        envelope.encrypt(new RSAEncrypter(publicKey.toRSAPublicKey()));
        return envelope.serialize();
    }

    private static String decrypt(byte[] body, SecretKey cek) throws Exception {
        JWEObject parsed = JWEObject.parse(new String(body, US_ASCII));
        parsed.decrypt(new DirectDecrypter(cek));
        return new String(parsed.getPayload().toBytes(), UTF_8);
    }
}
