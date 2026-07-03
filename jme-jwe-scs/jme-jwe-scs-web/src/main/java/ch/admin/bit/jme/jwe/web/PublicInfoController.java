package ch.admin.bit.jme.jwe.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Unencrypted request via the allowlist: {@code /api/public/**} is listed in
 * {@code jeap.jwe.filter.excluded-paths}, so this endpoint is exempt from JWE encryption and
 * served as plain JSON. It is still protected by jeap security — a valid Bearer token is
 * required, which shows that the allowlist only bypasses encryption, not authentication.
 */
@RestController
@RequestMapping("/api/public")
public class PublicInfoController {

    @GetMapping("/info")
    public Map<String, String> getInfo() {
        return Map.of(
                "application", "jme-jwe-scs",
                "serverTime", Instant.now().toString(),
                "transport", "plain JSON — /api/public/** is excluded from JWE encryption by the allowlist");
    }
}
