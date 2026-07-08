package ch.admin.bit.jme.jwe.web;

import ch.admin.bit.jeap.jwe.crypto.JweProtocolException;
import ch.admin.bit.jeap.jwe.crypto.JweRequestDecryptor;
import ch.admin.bit.jeap.jwe.crypto.JweResponseEncryptor;
import ch.admin.bit.jeap.jwe.keymanagement.JweKeyStore;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.crypto.DirectDecrypter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Base64;
import java.util.Map;

/**
 * Demo-only endpoint that decrypts a pasted compact JWE with the backend's private keys and
 * returns the protected header and the recovered plaintext, so the "Decrypt JWE" view can show
 * what actually travels inside an {@code application/jose} exchange captured from the network tab.
 *
 * <p><strong>Strictly for demonstration — never expose something like this in production.</strong>
 * It is a decryption oracle: any caller with the required role can decrypt arbitrary captured
 * JWEs addressed to this service. It exists only to make the encrypted transport of this example
 * tangible.
 *
 * <p>Request JWEs (encrypted with the backend's public JWKS keys, alg {@code RSA-OAEP-256}) are
 * decrypted directly. Response JWEs use {@code alg=dir} with the request-local CEK that the
 * backend never retains, so they can only be decrypted when the {@code JWE-Response-Key} envelope
 * of the same request is supplied as well: the backend unwraps the envelope with its private key
 * to recover the CEK — demonstrating how the response-key handshake works. Without the envelope,
 * the error response carries the parsed header so the UI can explain what is missing.
 *
 * <p>{@code /api/demo/**} lies within the JWE filter's included paths: the posted JWE arrives
 * wrapped in an encrypted request body, and the response containing the plaintext is encrypted
 * with the caller's response key — the decrypted content never crosses the wire in plaintext.
 * Error responses (4xx) are served as plain JSON because the JWE filter never encrypts error
 * responses.
 */
@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class DecryptDemoController {

    /**
     * The {@code alg} of response JWEs: direct encryption with the request-local response CEK.
     */
    private static final String DIR_ALGORITHM = "dir";

    /**
     * Demo-specific error reason: a response JWE was pasted without the {@code JWE-Response-Key}
     * envelope needed to recover its CEK.
     */
    private static final String RESPONSE_KEY_REQUIRED = "RESPONSE_KEY_REQUIRED";

    private final JweKeyStore jweKeyStore;

    /**
     * The pasted compact JWE to decrypt, plus — required for response JWEs only — the
     * {@code JWE-Response-Key} header value of the request the response belongs to.
     */
    public record DecryptDemoRequest(String jwe, String responseKeyEnvelope) {
    }

    public sealed interface DecryptDemoResponse permits DecryptDemoResult, DecryptDemoError {
    }

    /**
     * The parsed protected header and the recovered plaintext: {@code payloadText} when the
     * plaintext is valid UTF-8 (the usual JSON case), {@code payloadBase64} otherwise (e.g. the
     * raw AES key inside a {@code JWE-Response-Key} envelope).
     */
    public record DecryptDemoResult(Map<String, Object> header, String payloadText, String payloadBase64)
            implements DecryptDemoResponse {
    }

    /**
     * Protocol failure, with the parsed protected header when at least parsing succeeded (so the
     * UI can e.g. explain that a pasted response JWE (alg {@code dir}) additionally needs its
     * {@code JWE-Response-Key} envelope).
     */
    public record DecryptDemoError(String reason, String message, Map<String, Object> header)
            implements DecryptDemoResponse {
    }

    /**
     * Decrypting reveals person data, so the same role is required as for reading persons.
     */
    @PostMapping("/decrypt")
    @PreAuthorize("hasRole('person','read')")
    public ResponseEntity<DecryptDemoResponse> decrypt(@RequestBody DecryptDemoRequest request) {
        String compactJwe = request.jwe() == null ? "" : request.jwe().trim();
        if (compactJwe.isEmpty()) {
            throw new JweProtocolException(JweProtocolException.Reason.MALFORMED, "No JWE provided");
        }
        Map<String, Object> header = parseHeader(compactJwe);
        String envelope = request.responseKeyEnvelope() == null ? "" : request.responseKeyEnvelope().trim();
        if (DIR_ALGORITHM.equals(header.get("alg")) && envelope.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new DecryptDemoError(RESPONSE_KEY_REQUIRED,
                    "A response JWE (alg=dir) is encrypted with the request-local response key; supply the "
                            + "JWE-Response-Key header value of the same request to decrypt it",
                    header));
        }
        try {
            byte[] plaintext = DIR_ALGORITHM.equals(header.get("alg"))
                    ? decryptResponseJwe(compactJwe, envelope)
                    : JweRequestDecryptor.decrypt(compactJwe, jweKeyStore::findByKeyId).plaintext();
            String payloadText = tryDecodeUtf8(plaintext);
            return ResponseEntity.ok(new DecryptDemoResult(header, payloadText,
                    payloadText != null ? null : Base64.getEncoder().encodeToString(plaintext)));
        } catch (JweProtocolException e) {
            return badRequest(e, header);
        }
    }

    /**
     * Handles the failures raised before the header is available (blank input, unparseable JWE).
     */
    @ExceptionHandler(JweProtocolException.class)
    public ResponseEntity<DecryptDemoResponse> handleProtocolException(JweProtocolException e) {
        return badRequest(e, null);
    }

    /**
     * Decrypts a response JWE by first unwrapping the request-local CEK from the client's
     * {@code JWE-Response-Key} envelope — the same recovery the JWE servlet filter performs before
     * encrypting a response.
     */
    private byte[] decryptResponseJwe(String compactJwe, String responseKeyEnvelope) {
        SecretKey responseCek = JweResponseEncryptor.recoverResponseCek(responseKeyEnvelope, jweKeyStore::findByKeyId);
        try {
            JWEObject jwe = JWEObject.parse(compactJwe);
            jwe.decrypt(new DirectDecrypter(responseCek));
            return jwe.getPayload().toBytes();
        } catch (ParseException | JOSEException e) {
            throw new JweProtocolException(JweProtocolException.Reason.DECRYPTION_FAILED,
                    "Could not decrypt the response JWE with the CEK recovered from the JWE-Response-Key envelope", e);
        }
    }

    private static ResponseEntity<DecryptDemoResponse> badRequest(JweProtocolException e, Map<String, Object> header) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new DecryptDemoError(e.getReason().name(), e.getMessage(), header));
    }

    private static Map<String, Object> parseHeader(String compactJwe) {
        try {
            return JWEObject.parse(compactJwe).getHeader().toJSONObject();
        } catch (ParseException e) {
            throw new JweProtocolException(JweProtocolException.Reason.MALFORMED, "Could not parse compact JWE", e);
        }
    }

    private static String tryDecodeUtf8(byte[] plaintext) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(plaintext))
                    .toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }
}
