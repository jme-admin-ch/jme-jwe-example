package ch.admin.bit.jme.jwe;

import ch.admin.bit.jeap.jwe.test.JweTestKeys;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import ch.admin.bit.jeap.security.test.jws.JwsBuilder;
import ch.admin.bit.jeap.security.test.jws.JwsBuilderFactory;
import ch.admin.bit.jeap.security.test.resource.configuration.JeapOAuth2IntegrationTestResourceConfiguration;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.util.List;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * Integration test for the three showcased scenarios, using static JWE test keys instead of
 * Vault ({@code jeap.jwe.test.enabled=true}) so it runs without any infrastructure:
 * <ul>
 *     <li>Encrypted POST to {@code /api/persons}</li>
 *     <li>Encrypted GET of {@code /api/persons}</li>
 *     <li>Unencrypted (allowlisted) GET of {@code /api/public/info}</li>
 * </ul>
 * The full Docker Compose setup with Vault is covered by the {@code jme-jwe-test} module.
 *
 * <p>Additionally verifies that the SPA's {@code index.html} is served with the
 * {@code <base href>} rewritten to the servlet context path (see {@code IndexHtmlController}).
 *
 * <p>The JWE JWKS endpoint is moved to {@code /.well-known/jwe-jwks.json} for this test because
 * the jeap-security test support serves its token-signing JWKS at {@code /.well-known/jwks.json},
 * which would collide with the JWE JWKS endpoint's default path.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=28091",
                "jeap.jwe.test.enabled=true",
                "jeap.jwe.jwks.path=/.well-known/jwe-jwks.json",
                "spring.cloud.vault.enabled=false",
                "jeap.security.oauth2.resourceserver.authorization-server.issuer=" + JwsBuilder.DEFAULT_ISSUER,
                "jeap.security.oauth2.resourceserver.authorization-server.jwk-set-uri=http://localhost:28091/jme-jwe-scs/.well-known/jwks.json"
        })
@Import(JeapOAuth2IntegrationTestResourceConfiguration.class)
@SuppressWarnings("java:S112") // test helpers chain Nimbus/JCA calls with distinct checked exceptions
class JweScsIntegrationTest {

    private static final String BASE_URL = "http://localhost:28091/jme-jwe-scs";
    private static final String JWE_JWKS_PATH = "/.well-known/jwe-jwks.json";
    private static final String SUBJECT = "11111111-1111-1111-1111-111111111111";
    private static final String APPLICATION_JOSE = "application/jose";
    private static final String APPLICATION_JSON = "application/json";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String HEADER_ACCEPT = "Accept";
    private static final String HEADER_RESPONSE_KEY = "JWE-Response-Key";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ROLE_PERSON_READ = "jme_@person_#read";
    private static final String ROLE_PERSON_WRITE = "jme_@person_#write";

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private JwsBuilderFactory jwsBuilderFactory;

    @DynamicPropertySource
    static void staticKeys(DynamicPropertyRegistry registry) {
        // Two static keys mirror the two Vault transit key versions of the Docker Compose setup:
        // the last configured key is the newest version and is used for encryption, the older
        // version remains accepted for decryption (rotation grace).
        registry.add("jeap.jwe.test.keys[0]", () -> JweTestKeys.rsa4096Pem(0));
        registry.add("jeap.jwe.test.keys[1]", () -> JweTestKeys.rsa4096Pem(1));
    }

    // --- Showcased scenario 1: encrypted POST ----------------------------------------------------

    @Test
    void encryptedPostCreatesPersonAndReturnsEncryptedResponse() throws Exception {
        RSAKey publicKey = jwePublicKey();
        SecretKey responseCek = aes256();
        String personJson = "{\"firstName\":\"Max\",\"lastName\":\"Test\",\"ahvNumber\":\"756.0000.0000.00\"}";

        ResponseEntity<byte[]> response = client().post().uri("/api/persons")
                .header(HEADER_AUTHORIZATION, BEARER_PREFIX + token(ROLE_PERSON_WRITE))
                .header(HEADER_CONTENT_TYPE, APPLICATION_JOSE)
                .header(HEADER_ACCEPT, APPLICATION_JOSE)
                .header(HEADER_RESPONSE_KEY, responseKeyEnvelope(publicKey, responseCek))
                .body(encryptRequest(publicKey, personJson).getBytes(US_ASCII))
                .retrieve().toEntity(byte[].class);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(contentType(response)).isEqualTo(APPLICATION_JOSE);
        String decrypted = decrypt(response.getBody(), responseCek);
        assertThat(decrypted).contains("\"lastName\":\"Test\"").contains("\"id\":");
    }

    // --- Showcased scenario 2: encrypted GET -----------------------------------------------------

    @Test
    void encryptedGetReturnsPersonsEncryptedWithResponseKey() throws Exception {
        RSAKey publicKey = jwePublicKey();
        SecretKey responseCek = aes256();

        ResponseEntity<byte[]> response = client().get().uri("/api/persons")
                .header(HEADER_AUTHORIZATION, BEARER_PREFIX + token(ROLE_PERSON_READ))
                .header(HEADER_ACCEPT, APPLICATION_JOSE)
                .header(HEADER_RESPONSE_KEY, responseKeyEnvelope(publicKey, responseCek))
                .retrieve().toEntity(byte[].class);

        assertThat(contentType(response)).isEqualTo(APPLICATION_JOSE);
        assertThat(decrypt(response.getBody(), responseCek)).contains("Muster");
    }

    // --- Showcased scenario 3: unencrypted request via the allowlist -----------------------------

    @Test
    void allowlistedPublicInfoIsServedAsPlainJson() {
        ResponseEntity<String> response = client().get().uri("/api/public/info")
                .header(HEADER_AUTHORIZATION, BEARER_PREFIX + token())
                .retrieve().toEntity(String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(Objects.requireNonNull(response.getHeaders().getContentType()).toString())
                .startsWith(APPLICATION_JSON);
        assertThat(response.getBody()).contains("jme-jwe-scs");
    }

    @Test
    void allowlistedPublicInfoStillRequiresAuthentication() {
        HttpClientErrorException ex = catchThrowableOfType(HttpClientErrorException.class,
                () -> client().get().uri("/api/public/info").retrieve().toBodilessEntity());

        assertThat(ex.getStatusCode().value()).isEqualTo(401);
    }

    // --- Demo decrypt endpoint ----------------------------------------------------------------------

    @Test
    void decryptDemoEndpointRevealsHeaderAndPayloadOfACapturedRequestJwe() throws Exception {
        RSAKey publicKey = jwePublicKey();
        SecretKey responseCek = aes256();
        // A captured request JWE, as one would copy it from the browser's network tab
        String capturedRequestJwe = encryptRequest(publicKey, "{\"firstName\":\"Demo\",\"ahvNumber\":\"756.0000.0000.00\"}");

        ResponseEntity<byte[]> response = client().post().uri("/api/demo/decrypt")
                .header(HEADER_AUTHORIZATION, BEARER_PREFIX + token(ROLE_PERSON_READ))
                .header(HEADER_CONTENT_TYPE, APPLICATION_JOSE)
                .header(HEADER_ACCEPT, APPLICATION_JOSE)
                .header(HEADER_RESPONSE_KEY, responseKeyEnvelope(publicKey, responseCek))
                .body(encryptRequest(publicKey, "{\"jwe\":\"" + capturedRequestJwe + "\"}").getBytes(US_ASCII))
                .retrieve().toEntity(byte[].class);

        // The demo endpoint lies within the included paths, so its response is encrypted as well
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(contentType(response)).isEqualTo(APPLICATION_JOSE);
        String decrypted = decrypt(response.getBody(), responseCek);
        assertThat(decrypted)
                .contains("\"kid\":\"" + publicKey.getKeyID() + "\"")
                .contains("\"alg\":\"RSA-OAEP-256\"")
                .contains("\\\"firstName\\\":\\\"Demo\\\"")
                .contains("756.0000.0000.00");
    }

    @Test
    void decryptDemoEndpointDecryptsAResponseJweWithItsResponseKeyEnvelope() throws Exception {
        RSAKey publicKey = jwePublicKey();
        // A real encrypted response, captured together with the JWE-Response-Key header that
        // requested it — the envelope lets the backend recover the CEK the response was encrypted with
        SecretKey personsCek = aes256();
        String personsEnvelope = responseKeyEnvelope(publicKey, personsCek);
        byte[] personsResponseJwe = client().get().uri("/api/persons")
                .header(HEADER_AUTHORIZATION, BEARER_PREFIX + token(ROLE_PERSON_READ))
                .header(HEADER_ACCEPT, APPLICATION_JOSE)
                .header(HEADER_RESPONSE_KEY, personsEnvelope)
                .retrieve().toEntity(byte[].class).getBody();

        SecretKey demoCek = aes256();
        String body = "{\"jwe\":\"" + new String(Objects.requireNonNull(personsResponseJwe), US_ASCII)
                + "\",\"responseKeyEnvelope\":\"" + personsEnvelope + "\"}";
        ResponseEntity<byte[]> response = client().post().uri("/api/demo/decrypt")
                .header(HEADER_AUTHORIZATION, BEARER_PREFIX + token(ROLE_PERSON_READ))
                .header(HEADER_CONTENT_TYPE, APPLICATION_JOSE)
                .header(HEADER_ACCEPT, APPLICATION_JOSE)
                .header(HEADER_RESPONSE_KEY, responseKeyEnvelope(publicKey, demoCek))
                .body(encryptRequest(publicKey, body).getBytes(US_ASCII))
                .retrieve().toEntity(byte[].class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(decrypt(response.getBody(), demoCek))
                .contains("\"alg\":\"dir\"")
                .contains("Muster");
    }

    @Test
    void decryptDemoEndpointRequiresTheResponseKeyEnvelopeForResponseJwes() throws Exception {
        RSAKey publicKey = jwePublicKey();
        SecretKey responseCek = aes256();
        // A response-style JWE: dir/A256GCM with a request-local CEK the backend never retains
        JWEObject responseJwe = new JWEObject(
                new JWEHeader.Builder(JWEAlgorithm.DIR, EncryptionMethod.A256GCM)
                        .contentType(APPLICATION_JSON).build(),
                new Payload("{\"secret\":true}"));
        responseJwe.encrypt(new DirectEncrypter(aes256()));
        String body = "{\"jwe\":\"" + responseJwe.serialize() + "\"}";

        HttpClientErrorException ex = catchThrowableOfType(HttpClientErrorException.class,
                () -> client().post().uri("/api/demo/decrypt")
                        .header(HEADER_AUTHORIZATION, BEARER_PREFIX + token(ROLE_PERSON_READ))
                        .header(HEADER_CONTENT_TYPE, APPLICATION_JOSE)
                        .header(HEADER_ACCEPT, APPLICATION_JOSE)
                        .header(HEADER_RESPONSE_KEY, responseKeyEnvelope(publicKey, responseCek))
                        .body(encryptRequest(publicKey, body).getBytes(US_ASCII))
                        .retrieve().toBodilessEntity());

        // Error responses are never encrypted: plain JSON with the reason and the parsed header
        assertThat(ex.getStatusCode().value()).isEqualTo(400);
        assertThat(ex.getResponseBodyAsString())
                .contains("\"reason\":\"RESPONSE_KEY_REQUIRED\"")
                .contains("\"alg\":\"dir\"");
    }

    // --- Enforcement and authorization ------------------------------------------------------------

    @Test
    void plaintextPostToEncryptedPathIsRejected() {
        HttpClientErrorException ex = catchThrowableOfType(HttpClientErrorException.class,
                () -> client().post().uri("/api/persons")
                        .header(HEADER_AUTHORIZATION, BEARER_PREFIX + token(ROLE_PERSON_WRITE))
                        .header(HEADER_CONTENT_TYPE, APPLICATION_JSON)
                        .body("{\"firstName\":\"Plain\"}")
                        .retrieve().toBodilessEntity());

        assertThat(ex.getStatusCode().value()).isEqualTo(415);
        assertThat(ex.getResponseBodyAsString()).contains("\"code\":\"JWE_REQUEST_ENCRYPTION_REQUIRED\"");
    }

    @Test
    void getWithoutJoseAcceptIsRejected() {
        HttpClientErrorException ex = catchThrowableOfType(HttpClientErrorException.class,
                () -> client().get().uri("/api/persons")
                        .header(HEADER_AUTHORIZATION, BEARER_PREFIX + token(ROLE_PERSON_READ))
                        .header(HEADER_ACCEPT, APPLICATION_JSON)
                        .retrieve().toBodilessEntity());

        assertThat(ex.getStatusCode().value()).isEqualTo(406);
        assertThat(ex.getResponseBodyAsString()).contains("\"code\":\"JWE_RESPONSE_ENCRYPTION_REQUIRED\"");
    }

    @Test
    void personsEndpointRequiresPersonReadRole() throws Exception {
        RSAKey publicKey = jwePublicKey();
        SecretKey responseCek = aes256();

        HttpClientErrorException ex = catchThrowableOfType(HttpClientErrorException.class,
                () -> client().get().uri("/api/persons")
                        .header(HEADER_AUTHORIZATION, BEARER_PREFIX + token())
                        .header(HEADER_ACCEPT, APPLICATION_JOSE)
                        .header(HEADER_RESPONSE_KEY, responseKeyEnvelope(publicKey, responseCek))
                        .retrieve().toBodilessEntity());

        assertThat(ex.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void unauthenticatedRequestIsRejectedBeforeDecryption() {
        HttpClientErrorException ex = catchThrowableOfType(HttpClientErrorException.class,
                () -> client().get().uri("/api/persons")
                        .header(HEADER_ACCEPT, APPLICATION_JOSE)
                        .retrieve().toBodilessEntity());

        assertThat(ex.getStatusCode().value()).isEqualTo(401);
    }

    // --- Discovery endpoints ----------------------------------------------------------------------

    @Test
    void jweDiscoveryEndpointsAreReachableWithoutAuthentication() {
        assertThat(client().get().uri(JWE_JWKS_PATH)
                .retrieve().toBodilessEntity().getStatusCode().value()).isEqualTo(200);
        assertThat(client().get().uri("/.well-known/jwe-configuration")
                .retrieve().toBodilessEntity().getStatusCode().value()).isEqualTo(200);
    }

    // --- SPA frontend -----------------------------------------------------------------------------

    @Test
    void indexHtmlIsServedWithTheContextPathAsBaseHref() {
        ResponseEntity<String> response = client().get().uri("/")
                .retrieve().toEntity(String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(Objects.requireNonNull(response.getHeaders().getContentType()).toString())
                .startsWith("text/html");
        assertThat(response.getBody()).contains("<base href=\"/jme-jwe-scs/\"");
    }

    @Test
    void spaRouteIsForwardedToIndexHtmlWithTheContextPathAsBaseHref() {
        ResponseEntity<String> response = client().get().uri("/persons/edit/42")
                .retrieve().toEntity(String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("<base href=\"/jme-jwe-scs/\"");
    }

    // --- Multiple key versions ---------------------------------------------------------------------

    @Test
    void jwksServesBothKeyVersionsNewestFirst() throws Exception {
        List<RSAKey> publicKeys = jwePublicKeys();

        assertThat(publicKeys).hasSize(2);
        assertThat(publicKeys.get(0).getKeyID()).isEqualTo("jme-jwe-scs-key:2");
        assertThat(publicKeys.get(1).getKeyID()).isEqualTo("jme-jwe-scs-key:1");
    }

    @Test
    void encryptedGetWithPreviousKeyVersionStillWorks() throws Exception {
        // Rotation grace: a client that still encrypts with the previous key version (e.g. with a
        // cached JWKS from before a key rotation) is still served.
        RSAKey previousKey = jwePublicKeys().get(1);
        SecretKey responseCek = aes256();

        ResponseEntity<byte[]> response = client().get().uri("/api/persons")
                .header(HEADER_AUTHORIZATION, BEARER_PREFIX + token(ROLE_PERSON_READ))
                .header(HEADER_ACCEPT, APPLICATION_JOSE)
                .header(HEADER_RESPONSE_KEY, responseKeyEnvelope(previousKey, responseCek))
                .retrieve().toEntity(byte[].class);

        assertThat(contentType(response)).isEqualTo(APPLICATION_JOSE);
        assertThat(decrypt(response.getBody(), responseCek)).contains("Muster");
    }

    // --- Helpers ----------------------------------------------------------------------------------

    private String token(String... userRoles) {
        return jwsBuilderFactory.createValidForFixedLongPeriodBuilder(SUBJECT, JeapAuthenticationContext.USER)
                .withUserRoles(userRoles)
                .build().serialize();
    }

    private RestClient client() {
        return RestClient.create(BASE_URL);
    }

    private RSAKey jwePublicKey() throws Exception {
        return jwePublicKeys().get(0);
    }

    private List<RSAKey> jwePublicKeys() throws Exception {
        String jwks = client().get().uri(JWE_JWKS_PATH).retrieve().toEntity(String.class).getBody();
        return JWKSet.parse(Objects.requireNonNull(jwks)).getKeys().stream()
                .map(key -> (RSAKey) key)
                .toList();
    }

    private static SecretKey aes256() throws Exception {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(256);
        return generator.generateKey();
    }

    private static String encryptRequest(RSAKey publicKey, String json) throws Exception {
        JWEObject jwe = new JWEObject(
                new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A256GCM)
                        .keyID(publicKey.getKeyID()).contentType(APPLICATION_JSON).build(),
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
        JWEObject parsed = JWEObject.parse(new String(Objects.requireNonNull(body), US_ASCII));
        parsed.decrypt(new DirectDecrypter(cek));
        return new String(parsed.getPayload().toBytes(), UTF_8);
    }

    private static String contentType(ResponseEntity<byte[]> response) {
        return Objects.requireNonNull(response.getHeaders().getContentType()).toString();
    }
}
